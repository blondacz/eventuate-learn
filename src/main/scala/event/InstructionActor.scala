package event

import java.util

import akka.actor.{Actor, ActorRef}
import com.rbmhtechnology.eventuate.EventsourcedActor

class InstructionActor(override val id: String,
                       override val aggregateId: Option[String] = None,
                       override val eventLog: ActorRef) extends EventsourcedActor with ActorState[InstructionState] {
  println(s" starting Instruction actor $id")

  var state: InstructionState = InstructionState("New", "Unconfirmed", None)

  private var lastQuieried: Option[InstructionState] = None

  override def onCommand: Receive = {
    case GetStatus =>
      if (!lastQuieried.contains(state))
        println(s"  Instruction: $id status: $state => ${lastQuieried.orNull}")
      else
        println(s"      Instruction: $id status: $state")
      lastQuieried = Some(state)
  }

  override def onEvent: Receive = {
    case NewInstructionCreated(_, _, quantity) => state = state.copy(quantity = None)
  }

}

case class InstructionState(messageState: String, confirmationState: String, quantity: Option[BigDecimal])
