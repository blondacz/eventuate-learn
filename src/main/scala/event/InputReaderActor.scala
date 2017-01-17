package event

import akka.actor.{Actor, ActorRef}

import scala.io.Source


class InputReaderActor(val manager: ActorRef, primary: Boolean) extends Actor {
  final val ObligationCmd = "ob"
  final val StatusCmd = "status"

  private val lines = Source.stdin.getLines

  private val lineProcessing: PartialFunction[Any, List[String]] = {
    case line: String => line.split(' ').toList
  }

  private val mutatorProcessing: PartialFunction[List[String], Unit] = {
    case ObligationCmd :: "created" :: obRef :: quantity :: Nil => manager ! ContractualObligationCreated(obRef, BigDecimal(quantity))
    case ObligationCmd :: "cancelled" :: obRef :: Nil => manager ! ContractualObligationCancelled(obRef)
    case ObligationCmd :: "amended" :: obRef :: quantity :: Nil => manager ! ContractualObligationAmended(obRef, BigDecimal(quantity))
  }

  private val queryProcessing: PartialFunction[List[String], Unit] = {
    case StatusCmd :: Nil => context.actorSelection("../*") ! GetStatus()
    case StatusCmd :: obRef :: Nil => context.actorSelection(s"/user/manager/$obRef") ! GetStatus()
    case Nil =>
    case "" :: Nil =>
    case na :: nas => println(s"unknown command: $na $nas")
  }

  private val promptFunc: PartialFunction[Unit, Unit] = {
    case _ => prompt()
  }

  if (primary) {
    context.become(lineProcessing andThen (mutatorProcessing orElse queryProcessing) andThen promptFunc)
  }

  override def receive: Receive = lineProcessing andThen queryProcessing andThen promptFunc

  def prompt(): Unit = {
    if (lines.hasNext) lines.next() match {
      case "exit" => context.system.terminate()
      case line => self ! line
    }
  }

  override def preStart(): Unit = prompt()
}
