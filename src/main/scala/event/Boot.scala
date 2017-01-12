package event

import akka.actor.{ActorRef, ActorSystem, Props}
import com.rbmhtechnology.eventuate._
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog
import com.typesafe.config.ConfigFactory


object Boot extends App {

  val first = args(0) == "first"

  val firstPort: Int = 2252
  val secondPort: Int = 2253

  val config = loadConfig(if (first) firstPort else secondPort)

  implicit val system: ActorSystem = ActorSystem(ReplicationConnection.DefaultRemoteSystemName, config)

  println("Port is:" + config.getInt("akka.remote.netty.tcp.port"))
  println("Remote is:" + config.getString("akka.actor.provider"))
  val replicatedLog = if (first) startReplication("1", secondPort) else startReplication("2", firstPort)


  val root = system.actorOf(Props(new RootActor(replicatedLog)))
  if (!first) {
    root ! StartProcessing()
  }

  while (true) {
    Thread.sleep(10000)
    root ! QueryProcessing()
  }

  //  system.terminate()


  def startReplication(rid: String, port: Int, recover : Boolean = false) = {
//    println(s"Starting replication onf node $rid:", system.settings)
    val replicationEndpoint = new ReplicationEndpoint(id = rid, logNames = Set(ReplicationEndpoint.DefaultLogName),
      logFactory = logId => LeveldbEventLog.props(logId),
      connections = Set(ReplicationConnection("127.0.0.1", port)))

    //todo: this needs wait in case of recovery
    if (recover) replicationEndpoint.recover() else replicationEndpoint.activate()
    replicationEndpoint.logs(ReplicationEndpoint.DefaultLogName)
  }

  def loadConfig(port: Int) = {
    val myConfig = ConfigFactory.parseString(
        s"""akka.actor.provider = "akka.remote.RemoteActorRefProvider"
            |akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
            |eventuate.log.leveldb.dir = "target/location-$port"
            |akka.remote.netty.tcp.hostname = "127.0.0.1"
            |akka.remote.netty.tcp.port=$port""".stripMargin)
    // load the normal config stack (system props,
    // then application.conf, then reference.conf)
    val regularConfig = ConfigFactory.load()
    // override regular stack with myConfig
    val combined = myConfig.withFallback(regularConfig)
    // put the result in between the overrides
    // (system props) and defaults again
    ConfigFactory.load(combined)
  }

}

