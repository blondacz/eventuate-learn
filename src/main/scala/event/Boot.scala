package event

import akka.actor.{ActorSystem, Props}
import akka.pattern.AskSupport
import akka.stream.ActorMaterializer
import com.rbmhtechnology.eventuate._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future.successful
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}


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
      val snapshotManager = system.actorOf(Props[SnapshotManagerActor], "snapshot-manager")
      val input = system.actorOf(Props(new InputReaderActor(manager, primary)))
      if (primary) {
        val kafkaReader = system.actorOf(Props(new KafkaReaderActor("kfr", Some("kfr0"), eventLog, manager)))
        kafkaReader ! InitReading
        val kafkaWriter = system.actorOf(Props(new KafkaWriterActor(eventLog)))
      }
      system.scheduler.schedule(1 minute, 1 minute, snapshotManager, CaptureSnapshot)
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
         |eventuate.snapshot.filesystem.dir = "target/snapshots-$port"
         |
         |akka {
         |
         |actor {
         |  serializers {
         |    domain-event-serializer = "event.CustomStringSerializer"
         |    kryo = "com.twitter.chill.akka.AkkaSerializer"
         |  }
         |  provider = "akka.remote.RemoteActorRefProvider"
         |
         |  serialization-bindings {
         |   "java.io.Serializable" = none
         |    "event.EventRead" = domain-event-serializer
         |    "scala.Product" = kryo
         |  }
         |}
         |
         |
         |kafka.consumer.kafka-clients {
         |  enable.auto.commit = true
         |  auto.commit.interval.ms = 10000
         |}
         |
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

