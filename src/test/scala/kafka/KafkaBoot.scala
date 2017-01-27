package kafka

import net.manub.embeddedkafka.EmbeddedKafka
import org.scalacheck.Gen
import org.scalatest.FreeSpec

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

class KafkaBoot extends FreeSpec with EmbeddedKafka {

  "runs kafka until killed" in {
    EmbeddedKafka.start()

    readOutboundMessages("instruction-events")
    writeInboundMessages("obligation-events")

    EmbeddedKafka.stop()
  }

  private def writeInboundMessages(inboundTopic: String): Unit = {
    var i = 1

    while (true) {

      publishStringMessageToKafka(inboundTopic, commandGen.sample.get)
      i += 1
      Thread.sleep(1000)
    }
  }

  private def readOutboundMessages(outboundTopic: String): Future[Unit] = {
    var i = 1

    Future {
      while (true) {
        val msg = Try(consumeFirstStringMessageFrom(outboundTopic))
        println(s"Received: $i / $msg")
        i += 1
      }
    }
  }

  private val commandGen: Gen[String] = for {
    obRef <- Gen.alphaLowerChar
    value <- Gen.chooseNum(100, 200)
    command <- Gen.oneOf(s"created $obRef $value", s"amended $obRef $value", s"cancelled $obRef")
  } yield s"obligation $command"

}
