package event

import akka.actor.{Actor, ActorRef, Props}
import com.rbmhtechnology.eventuate.{EventsourcedActor, EventsourcedView}

import scala.collection.mutable
import scala.util.{Failure, Success}

class ManagerActor(override val id : String,
                   override val eventLog: ActorRef) extends EventsourcedView {
  private var actors = mutable.Map[String, ActorRef]()


  override def onCommand: Receive = {
    case CaptureSnapshot =>
      save(actors) {
        case Success(metadata) =>
          sender() ! SnapshotSaveSuccess(metadata)
        case Failure(cause) =>
          sender() ! SnapshotSaveFailure(Some("manager"), cause)
      }
    case Hi(obRef) => actors += (obRef -> sender())
    case GetStatus => println("Manager:" + actors)
      actors.foreach {
        case (_,ref) => ref ! GetStatus
      }
    case ContractualObligationCreated(obRef, quantity) => findActor(obRef) ! Instruct(quantity)
    case ContractualObligationAmended(obRef, quantity) => findActor(obRef) ! Amend(quantity)
    case ContractualObligationCancelled(obRef) => findActor(obRef) ! Cancel
  }

  override def onEvent: Receive = {
    case o: ObligationLifecycleEvent => findActor(o.obRef)
  }

  def createActor(obRef: String): ActorRef =
    context.actorOf(Props(new ObligationActor(obRef,Some(obRef),eventLog) ), obRef )

  def findActor(obRef: String): ActorRef =
    actors.getOrElseUpdate(obRef, createActor(obRef))


  override def onSnapshot: Receive = {
    case s: mutable.Map[String, ActorRef] =>
      println(s"$aggregateId Restoring from snapshot $s")
      actors = s
  }

}
