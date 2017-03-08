package event

import akka.actor.{Actor, ActorRef}
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.rbmhtechnology.eventuate.adapter.stream.DurableEventSource
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}


class KafkaWriterActor(eventLog: ActorRef) extends Actor {

  implicit val mat = ActorMaterializer()

  var traceAlreadyReceived = false

  var traceName = "no-trace"


  override def preStart(): Unit = {
    traceOffset(context.self)
  }

  override def receive: Receive = {
    case StartedReading =>
      sender ! AckReading
      println("Working out offset")
    case e: CommittableMessage[String, String] =>
      println(s"Received last message in Writer ${e.record.value()}")
      sender() ! ReadingComplete
      val offset = e.record.value().split("\\|").head
      startWritingToKafka(offset.toString.toLong + 1)
    case x => println(s"Writer Not handling: $x")
  }


  private val topic = new TopicPartition("instruction-events", 0)

  def traceOffset(kafkaWriter: ActorRef) = {
    val consumerSettings = ConsumerSettings(context.system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:6001")
      .withGroupId(s"system-writter-trace")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

    val kafkaConsumer: KafkaConsumer[String, String] = consumerSettings.createKafkaConsumer()

    import scala.collection.JavaConverters._
    kafkaConsumer.assign(List(topic).asJava)
    kafkaConsumer.seekToEnd(List(topic).asJava)
    val endsPosition: Long = kafkaConsumer.position(topic)
    if (endsPosition > 0) kafkaConsumer.seek(topic, endsPosition - 1)
    val secondLastPosition: Long = kafkaConsumer.position(topic)

    if (secondLastPosition != endsPosition) {

      Consumer.committableSource(consumerSettings, Subscriptions.assignmentWithOffset(topic, endsPosition - 1))
        .runWith(Sink.actorRefWithAck(kafkaWriter, StartedReading, AckReading, ReadingComplete))
    } else {
      startWritingToKafka(0)
    }
  }

  val producerSettings = ProducerSettings(context.system, new StringSerializer, new StringSerializer)
    .withBootstrapServers("localhost:6001")

  private def startWritingToKafka(offset: Long) = {
    println(s"Starting writing to Kafka with offset: $offset")

    val source = Source.fromGraph(DurableEventSource(eventLog, fromSequenceNr = offset))
    source.filter(_.payload match {
      case EventRead(_) => false
      case _ => true
    }).map { elem =>
      new ProducerRecord[String, String]("instruction-events", elem.localSequenceNr + "|" + elem.payload.toString)
    }.runWith(Producer.plainSink(producerSettings))
  }
}
