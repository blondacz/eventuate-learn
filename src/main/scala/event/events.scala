package event

import akka.actor.ActorRef
import com.rbmhtechnology.eventuate.SnapshotMetadata

//external
case class ContractualObligationCreated(obRef: String, quantity : BigDecimal)
case class ContractualObligationCancelled(obRef: String)
case class ContractualObligationAmended(obRef: String, quantity : BigDecimal)

//commands
case class Instruct(quantity : BigDecimal)
case class Amend(quantity : BigDecimal)
case object Cancel

//internal events
trait ObligationLifecycleEvent {
  def obRef : String
}
case class InstructingStarted(obRef : String, quantity : BigDecimal) extends ObligationLifecycleEvent
case class Amended(obRef : String, quantity : BigDecimal) extends ObligationLifecycleEvent
case class Cancelled(obRef : String) extends ObligationLifecycleEvent


//actor communication
case class Hi(obRef: String)
case object GetStatus

sealed trait Snapshotting
case object CaptureSnapshot extends Snapshotting
case class SnapshotSaveSuccess(metadata: SnapshotMetadata) extends Snapshotting
case class SnapshotSaveFailure(aggregateId : Option[String] ,cause: Throwable) extends Snapshotting


case object GetOffset
case object InitReading
case object StartedReading
case object AckReading
case object ReadingComplete
case class EventRead(offset : Long)


