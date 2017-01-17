package event

import akka.actor.{ActorContext, ActorRef}
import com.rbmhtechnology.eventuate.EventsourcedActor

class ObligationActor(override val id: String,
                      override val aggregateId : Option[String] = None,
                      override val eventLog: ActorRef) extends EventsourcedActor {
  private var state : String = "New"

  override def onCommand: Receive = {
    case GetStatus() =>  println(s"Obligation: $id status: $state")
    case Instruct(quantity) => persist(InstructingStarted(id, quantity)) { _ => }
    case Amend(quantity) => persist(Amended(id, quantity)) { _ => }
    case Cancel() => persist(Cancelled(id)) { _ => }
  }

  override def onEvent: Receive = {
    case InstructingStarted(_, quantity) => state = "InstructingStarted"
    case Amended(_, quantity) => state = "Amended"
    case Cancelled(_) => state = "Cancelled"
  }
}
