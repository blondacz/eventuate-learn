package event

import akka.actor.{ActorContext, ActorRef}
import com.rbmhtechnology.eventuate.EventsourcedActor

class ObligationActor(override val id: String,
                      override val aggregateId : Option[String] = None,
                      override val eventLog: ActorRef) extends EventsourcedActor {
  type ObligationState = (String, Map[BigDecimal, BigDecimal])
  private var state : ObligationState = ("New",Map(BigDecimal(0) -> 0))

  override def onCommand: Receive = {
    case CaptureSnapshot =>
      println(s"capturing snapshot for $aggregateId")
      save(state)(e => println(s"Sanpshoting result: $e"))
    case GetStatus =>  println(s"Obligation: $id status: $state")
    case Instruct(quantity) => persist(InstructingStarted(id, quantity)) { _ => }
    case Amend(quantity) => persist(Amended(id, quantity)) { _ => }
    case Cancel => persist(Cancelled(id)) { _ => }
  }

  override def onEvent: Receive = {
    case InstructingStarted(_, quantity) => state = ("InstructingStarted",Map(state._2.values.head -> quantity))
    case Amended(_, quantity) => state = ("Amended",Map(state._2.values.head -> quantity))
    case Cancelled(_) => state = ("Cancelled",Map( state._2.values.head -> 0))
  }

  override def onSnapshot: Receive = {
    case s: ObligationState =>
      println(s"$aggregateId Restoring from snapshot $s")
      state = s
  }
}
