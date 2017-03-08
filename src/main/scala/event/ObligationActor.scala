package event

import akka.actor.ActorRef
import com.rbmhtechnology.eventuate.{EventsourcedActor, SnapshotMetadata}

import scala.util.{Failure, Success}

class ObligationActor(override val id: String,
                      override val aggregateId: Option[String] = None,
                      override val eventLog: ActorRef) extends EventsourcedActor {
  type ObligationState = (String, Map[BigDecimal, BigDecimal])
  private var state: ObligationState = ("New", Map(BigDecimal(0) -> 0))

  override def onCommand: Receive = {
    case CaptureSnapshot =>
      println("Snapshotting")
      save(state) {
        case Success(metadata) =>
          sender() ! SnapshotSaveSuccess(metadata)
        case Failure(cause) =>
          sender() ! SnapshotSaveFailure(aggregateId,cause)
      }
    case GetStatus => println(s"Obligation: $id status: $state")
    case Instruct(quantity) => persist(InstructingStarted(id, quantity)) { _ => }
    case Amend(quantity) => persist(Amended(id, quantity)) { _ => }
    case Cancel => persist(Cancelled(id)) { _ => }
  }

  override def onEvent: Receive = {
    case InstructingStarted(_, quantity) => state = ("InstructingStarted", Map(state._2.values.head -> quantity))
    case Amended(_, quantity) => state = ("Amended", Map(state._2.values.head -> quantity))
    case Cancelled(_) => state = ("Cancelled", Map(state._2.values.head -> 0))
  }

  override def onSnapshot: Receive = {
    case s: ObligationState =>
      println(s"$aggregateId Restoring from snapshot $s")
      state = s
  }
}
