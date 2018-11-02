package event

import akka.actor.{ActorSystem, DeadLetter, Props}
import akka.pattern.AskSupport
import akka.stream.ActorMaterializer
import com.rbmhtechnology.eventuate._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
//import com.rbmhtechnology.eventuate.tools.metrics.kamon.KamonReplicationMetricsRecorder
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions}
//import kamon.Kamon

import scala.concurrent.Future.successful
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}


object Boot extends App with AskSupport {
  val primary = args.headOption match {
    case Some("primary") => true
    case _ => false
  }

  val config = ConfigFactory.load()
  logConfig

//  Kamon.start(config)

  implicit val system: ActorSystem = ActorSystem(ReplicationConnection.DefaultRemoteSystemName, config)
  implicit val mat = ActorMaterializer()


  val replicationPort: Int = sys.props.get("replicationPort").map(_.toInt).getOrElse(0)

  val initialization = startReplication(replicationPort)

  import system.dispatcher

  initialization.onComplete {
    case Failure(e) =>
      println(s"Recovery failed: ${e.getMessage}")
      system.terminate()
    case Success(eventLog) =>

      val manager = system.actorOf(Props(new ManagerActor("man", eventLog)), "manager")
      val snapshotManager = system.actorOf(Props[SnapshotManagerActor], "snapshot-manager")
      val input = system.actorOf(Props(new InputReaderActor(manager, primary)))
      val kafkaReader = system.actorOf(Props(new KafkaReaderActor("kfr", Some("kfr0"), eventLog, manager, primary)),"input-reader")
      if (primary) {
        kafkaReader ! InitReading
        val kafkaWriter = system.actorOf(Props(new KafkaWriterActor(eventLog)),"output-writer")
      }
      system.eventStream.subscribe(snapshotManager,classOf[DeadLetter])
      system.scheduler.schedule(1 minute, 60 seconds, snapshotManager, CaptureSnapshot)
  }


  def startReplication(port: Int, recover: Boolean = false) = {
    val replicationEndpoint = new ReplicationEndpoint(id = port.toString, logNames = Set(ReplicationEndpoint.DefaultLogName),
      logFactory = logId => LeveldbEventLog.props(logId),
      connections = Set(ReplicationConnection("127.0.0.1", port)))
//    val metrics = new KamonReplicationMetricsRecorder(replicationEndpoint, Some("eventlog."))
    (if (recover) replicationEndpoint.recover() else successful(replicationEndpoint.activate()))
      .map(x => replicationEndpoint.logs(ReplicationEndpoint.DefaultLogName)
    )
  }

  def logConfig: Unit = {
    println("Starting as primary:" + primary)
    println("System Port is:" + sys.props.get("port"))
    println("Replication Port is:" + sys.props.get("replicationPort"))
    println(config.root().render(ConfigRenderOptions.concise()))
  }
}

