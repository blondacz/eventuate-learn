package event

//external
case class ContractualObligationCreated(obRef: String, quantity : BigDecimal)
case class ContractualObligationCancelled(obRef: String)
case class ContractualObligationAmended(obRef: String, quantity : BigDecimal)

//commands
case class Instruct(quantity : BigDecimal)
case class Amend(quantity : BigDecimal)
case class Cancel()

//internal events
trait ObligationLifecycleEvent {
  def obRef : String
}
case class InstructingStarted(obRef : String, quantity : BigDecimal) extends ObligationLifecycleEvent
case class Amended(obRef : String, quantity : BigDecimal) extends ObligationLifecycleEvent
case class Cancelled(obRef : String) extends ObligationLifecycleEvent


//actor communication
case class Hi(obRef: String)
case class GetStatus()

