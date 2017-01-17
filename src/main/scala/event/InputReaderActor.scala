package event

import akka.actor.{Actor, ActorRef}

import scala.io.Source


class InputReaderActor(val manager: ActorRef, active: Boolean) extends Actor {
  val lines = Source.stdin.getLines

  if (!active) {
    context.become {
      case line: String => line.split(' ').toList match {
        case "status" :: Nil => context.actorSelection("../*") ! GetStatus(); prompt()
        case "status" :: obRef :: Nil => context.actorSelection(s"/user/manager/$obRef") ! GetStatus(); prompt()
        case Nil => prompt()
        case "" :: Nil => prompt()
        case na :: nas => println(s"unknown command: $na $nas"); prompt()
      }
    }
  }

  override def receive: Receive = {
    case line: String => line.split(' ').toList match {
      case "obligation" :: "created" :: obRef :: quantity :: Nil => manager ! ContractualObligationCreated(obRef, BigDecimal(quantity)); prompt()
      case "obligation" :: "cancelled" :: obRef :: Nil => manager ! ContractualObligationCancelled(obRef); prompt()
      case "obligation" :: "amended" :: obRef :: quantity :: Nil => manager ! ContractualObligationAmended(obRef, BigDecimal(quantity)); prompt()
      case "status" :: Nil => context.actorSelection("../*") ! GetStatus(); prompt()
      case "status" :: obRef :: Nil => context.actorSelection(s"/user/manager/$obRef") ! GetStatus(); prompt()
      case Nil => prompt()
      case "" :: Nil => prompt()
      case na :: nas => println(s"unknown command: $na $nas"); prompt()
    }
  }

  def prompt(): Unit = {
    if (lines.hasNext) lines.next() match {
      case "exit" => context.system.terminate()
      case line => self ! line
    }
  }

  override def preStart(): Unit = prompt()
}
