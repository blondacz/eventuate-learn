package event

import akka.actor.{Actor, ActorRef, Props}
import com.rbmhtechnology.eventuate.log.leveldb.LeveldbEventLog

import scala.util.Random

/**
  * Created by vagrant on 11/01/17.
  */
class RootActor(val eventLog: ActorRef) extends Actor {
  val system = context

  val a1 = system.actorOf(Props(new ExampleActor("1", Some("a"), eventLog)))
  val a2 = system.actorOf(Props(new ExampleActor("2", Some("a"), eventLog)))
  val b1 = system.actorOf(Props(new ExampleActor("3", Some("b"), eventLog)))

  val v = system.actorOf(Props(new ExampleView("4", eventLog)))


  def sendMessages(): Unit = {
    a1 ! Append("1")

    a2 ! Append("2")
    b1 ! Append("3")
  }

  def printMessages(): Unit = {
    a1 ! Print
    a2 ! Print
    b1 ! Print
    v ! GetAppendCount
  }

  override def receive: Receive = {
    case StartProcessing() => sendMessages()
    case QueryProcessing() => printMessages()
    case e => println("Received:" + e)
  }
}
