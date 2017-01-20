package event

import akka.actor.ActorRef
import akka.actor.Status.Failure
import akka.kafka.ConsumerMessage.CommittableMessage
import com.rbmhtechnology.eventuate.EventsourcedActor

import scala.util.Success


class KafkaReaderActor(override val id: String,
                       override val aggregateId : Option[String],
                       override val eventLog: ActorRef,
                       val manager: ActorRef) extends EventsourcedActor {

  var offset : Long = 0L

  override def onCommand: Receive  = {
    case GetOffset => sender() ! LastOffset(offset)
    case InitReading => println(s"Initializing reading")
      sender ! AckReading
    case e: CommittableMessage[String, String] =>
      offset = e.record.offset()
      val value: String = e.record.value()
      val command = value.split(" ").toList match {
        case _ :: "created" :: obRef :: ammount :: Nil  => ContractualObligationCreated(obRef,BigDecimal(ammount))
        case _ :: "amended" :: obRef :: ammount :: Nil  => ContractualObligationAmended(obRef,BigDecimal(ammount))
        case _ :: "cancelled" :: obRef :: Nil  => ContractualObligationCancelled(obRef)
      }
      println(s"Message: ${e.record.offset()}/ $value")
      persist(EventRead(offset)) {
        case Success(evt) =>
          // success reply
          manager ! command
          sender() ! AckReading
        case _ => println("failed to save event")
      }
  }

  override def onEvent: Receive = {
    case EventRead(off) => offset = off
  }
}
