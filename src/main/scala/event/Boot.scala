package event

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.kafka.Subscriptions.assignmentWithOffset
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.pattern.AskSupport
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.rbmhtechnology.eventuate._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.Future.successful
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.language.postfixOps


object Boot extends App with AskSupport {


  val primary = args.headOption match {
    case Some("primary") => true
    case _ => false
  }

  val firstPort: Int = 2252
  val secondPort: Int = 2253

  val config = loadConfig(if (primary) firstPort else secondPort)

  implicit val system: ActorSystem = ActorSystem(ReplicationConnection.DefaultRemoteSystemName, config)
  implicit val mat = ActorMaterializer()

  println("Port is:" + config.getInt("akka.remote.netty.tcp.port"))
  println("Remote is:" + config.getString("akka.actor.provider"))
  val initialization = if (primary) startReplication("1", secondPort) else startReplication("2", firstPort)

  import system.dispatcher

  initialization.onComplete {
    case Failure(e) =>
      println(s"Recovery failed: ${e.getMessage}")
      system.terminate()
    case Success(eventLog) =>
      val manager = system.actorOf(Props(new ManagerActor("man", eventLog)), "manager")
      val input = system.actorOf(Props(new InputReaderActor(manager, primary)))
      if (!primary) {
        readFromKafka(eventLog, manager)
      }
  }


  def readFromKafka(eventLog: ActorRef, manager: ActorRef): Unit = {
    val kafkaReader = system.actorOf(Props(new KafkaReaderActor("kfr", Some("kfr0"), eventLog, manager)))
    implicit val timeout = akka.util.Timeout(5 seconds)

    (kafkaReader ? GetOffset()).mapTo[LastOffset].onSuccess {
      case LastOffset(offset) =>
        val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
          .withBootstrapServers("localhost:6001")
          .withGroupId(s"system-$firstPort")
          .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

        Consumer.committableSource(consumerSettings, assignmentWithOffset(new TopicPartition("obligation-events", 0), offset))
          .runWith(Sink.actorRefWithAck(kafkaReader, InitReading(), AckReading(), ReadingComplete()))
    }
  }

  def startReplication(rid: String, port: Int, recover: Boolean = false) = {
    val replicationEndpoint = new ReplicationEndpoint(id = rid, logNames = Set(ReplicationEndpoint.DefaultLogName),
      logFactory = logId => LeveldbEventLog.props(logId),
      connections = Set(ReplicationConnection("127.0.0.1", port)))

    (
      if (recover) replicationEndpoint.recover() else successful(replicationEndpoint.activate())
      ).map(x => replicationEndpoint.logs(ReplicationEndpoint.DefaultLogName)
    )
  }

  def loadConfig(port: Int) = {
    val myConfig = ConfigFactory.parseString(
      s"""
         |eventuate.log.leveldb.dir = "target/location-$port"
         |akka {
         |kafka.consumer.kafka-clients {
         |  enable.auto.commit = true
         |  auto.commit.interval.ms = 10000
         |}
         | actor.provider = "akka.remote.RemoteActorRefProvider"
         | remote {
         |  enabled-transports = ["akka.remote.netty.ssl"]
         |  netty.ssl{
         |    enable-ssl = true
         |    hostname = "127.0.0.1"
         |    port=$port
         |    security {
         |      key-store = "src/main/resources/keystore"
         |      key-store-password = "09040407050407080702010C0903090D0C0E0906"
         |      key-password = "09040407050407080702010C0903090D0C0E0906"
         |      trust-store = "src/main/resources/truststore"
         |      trust-store-password = "09040407050407080702010C0903090D0C0E0906"
         |      protocol = "TLSv1"
         |      random-number-generator = "AES128CounterSecureRNG"
         |      enabled-algorithms = ["TLS_RSA_WITH_AES_128_CBC_SHA"]
         |    }
         |  }
         | }
         |}""".stripMargin)
    val regularConfig = ConfigFactory.load()
    val combined = myConfig.withFallback(regularConfig)
    ConfigFactory.load(combined)
  }
}

