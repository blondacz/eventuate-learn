package event

import java.util

import akka.actor.{ActorContext, ActorRef, Props}
import com.rbmhtechnology.eventuate.EventsourcedActor

import scala.util.{Failure, Success}

class ObligationActor(override val id: String,
                      override val aggregateId: Option[String] = None,
                      override val eventLog: ActorRef) extends EventsourcedActor with ActorState[ObligationState] {
  var state: ObligationState = ObligationState(id, "New", 1, List.empty, "0")

  private var lastQueried: Option[ObligationState] = None
  println(s" starting Obligation actor $id")


  override def onCommand: Receive = {
    case CaptureSnapshot =>
      println("Snapshotting")
      save(state) {
        case Success(metadata) =>
          sender() ! SnapshotSaveSuccess(metadata)
        case Failure(cause) =>
          sender() ! SnapshotSaveFailure(aggregateId, cause)
      }
    case GetStatus =>
      if (!lastQueried.contains(state))
        println(s"Obligation: $id status: $state => ${lastQueried.orNull}")
      else
        println(s"    Obligation: $id status: $state")
      lastQueried = Some(state)
      context.children.foreach(_ ! GetStatus)


    case NewObligation(quantity) =>
      val newId = generateInstructionId(state)
      persist(NewInstructionCreated(id, newId, quantity),Set(newId)) { _ => }
    case Amend(quantity) => persist(Amended(id, quantity)) { _ => }
    case Cancel => persist(Cancelled(id)) { _ => }
  }

  override def onEvent: Receive = {
    case NewInstructionCreated(_, instructionId, quantity) =>
      createActor(instructionId)
      state = state.instruct(quantity, instructionId)
    case Amended(_, quantity) => state = state.amend(quantity)
    case Cancelled(_) => state = state.cancel
  }

  def createActor(instructionId: String): Unit = context.actorOf(Props(new InstructionActor(instructionId,Some(instructionId),eventLog) ), instructionId )

  override def onSnapshot: Receive = {
    case s: ObligationState =>
      println(s"$aggregateId Restoring obligation from snapshot $s")
      state = s
      s.instructions.foreach(createActor)
  }

  def generateInstructionId(state: ObligationState) : String = {
    val newId = state.obRef  + "-" + (state.instructions.size + 1)
    println(s" Generating new id $newId for $state ")
    newId
  }
}

case class ObligationState(obRef: String, state: String, version : Long, instructions: List[String], desc: String) {
  def amend(quantity: BigDecimal) : ObligationState = copy(state = "Amended",version = version + 1, desc = quantity.toString)
  def cancel : ObligationState = copy(state = "Cancelled",version = version + 1, desc = "")
  def instruct(quantity: BigDecimal, instructionId: String) : ObligationState = copy(state = "InstructingStarted",version = version + 1, desc = quantity.toString, instructions = instructionId :: instructions)
}
