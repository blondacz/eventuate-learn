package event

import scala.util._
import akka.actor._
import com.rbmhtechnology.eventuate.{EventsourcedActor, VectorTime}

// Commands
case object Print
case class Append(entry: String)

// Command replies
case class AppendSuccess(entry: String)
case class AppendFailure(cause: Throwable)

// Event
case class Appended(entry: String)

class ExampleActor(override val id: String,
                   override val aggregateId: Option[String],
                   override val eventLog: ActorRef) extends EventsourcedActor {

  private var currentState: Vector[String] = Vector.empty
  private var updateTimestamp: VectorTime = VectorTime()

  override def onCommand = {
    case Print =>
      println(s"[id = $id, aggregate id = ${aggregateId.getOrElse("<undefined>")}] ${currentState.mkString(",")}")
    case Append(entry) => persist(Appended(entry)) {
      case Success(evt) => sender() ! AppendSuccess(entry)
      case Failure(err) => sender() ! AppendFailure(err)
    }
  }

  override def onEvent = {
    case Appended(entry) =>
      if (updateTimestamp < lastVectorTimestamp) {
        // regular update
        currentState = currentState :+ entry
        updateTimestamp = lastVectorTimestamp
      } else if (updateTimestamp conc lastVectorTimestamp) {
        println("Confilcting versions detected....")
        currentState = currentState :+ "Conflict"

      }
  }
}

import akka.actor.ActorRef
import com.rbmhtechnology.eventuate.EventsourcedView
import com.rbmhtechnology.eventuate.VectorTime


case object GetAppendCount
case class GetAppendCountReply(count: Long)

class ExampleView(override val id: String, override val eventLog: ActorRef) extends EventsourcedView {
  private var appendCount: Long = 0L

  override def onCommand = {
    case GetAppendCount => sender() ! GetAppendCountReply(appendCount)
  }

  override def onEvent = {
    case Appended(_) => appendCount += 1L
  }
}
