package event

import akka.actor.ActorRef
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.ConsumerSettings
import akka.kafka.Subscriptions._
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.rbmhtechnology.eventuate.EventsourcedActor
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.util.Success


class KafkaReaderActor(override val id: String,
                       override val aggregateId: Option[String],
                       override val eventLog: ActorRef,
                       val manager: ActorRef) extends EventsourcedActor {

  var offset: Long = 0L


  override def onCommand: Receive = {
    case InitReading =>
      readFromKafka()
    case StartedReading =>
      println(s"Initializing reading")
      sender() ! AckReading
    case e: CommittableMessage[String, String] =>
      offset = e.record.offset()
      val value: String = e.record.value()
      val command = value.split(" ").toList match {
        case _ :: "created" :: obRef :: ammount :: Nil => ContractualObligationCreated(obRef, BigDecimal(ammount))
        case _ :: "amended" :: obRef :: ammount :: Nil => ContractualObligationAmended(obRef, BigDecimal(ammount))
        case _ :: "cancelled" :: obRef :: Nil => ContractualObligationCancelled(obRef)
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

  def readFromKafka(): Unit = {
    implicit val mat = ActorMaterializer()

    val consumerSettings = ConsumerSettings(context.system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:6001")
      .withGroupId(s"system-primary")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    println(s"Reading from Kafka from offset: ${offset + 1}")
    Consumer.committableSource(consumerSettings, assignmentWithOffset(new TopicPartition("obligation-events", 0), offset + 1))
      .runWith(Sink.actorRefWithAck(context.self, StartedReading, AckReading, ReadingComplete))
  }

}
