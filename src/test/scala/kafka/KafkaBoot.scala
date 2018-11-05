package kafka

import net.manub.embeddedkafka.EmbeddedKafka
import org.scalacheck.Gen
import org.scalatest.{BeforeAndAfterAll, FreeSpec}

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

class KafkaBoot extends FreeSpec with EmbeddedKafka with BeforeAndAfterAll {

  private val messagePause = 5

  override def beforeAll() {
    EmbeddedKafka.start()
  }

  override def afterAll() {
    EmbeddedKafka.stop()
  }

  "runs kafka until killed" in {
    readOutboundMessages("instruction-events")
    writeInboundMessages("obligation-events")
  }

  private def writeInboundMessages(inboundTopic: String): Unit = {
    var i = 1

    while (true) {

      publishStringMessageToKafka(inboundTopic, commandGen.sample.get)
      i += 1
      Thread.sleep(messagePause * 1000)
    }
  }

  private def readOutboundMessages(outboundTopic: String): Future[Unit] = {
    var i = 1

    Future {
      while (true) {
        val msg = Try(consumeFirstStringMessageFrom(outboundTopic))
        msg.map { m =>
          println(s"Received: $i / $m")
          i += 1
        }.failed.foreach( "   No MSG on output because:" + _)

      }
    }
  }

  private val commandGen: Gen[String] = for {
    obRef <- Gen.alphaLowerChar
    value <- Gen.chooseNum(100, 200)
    command <- Gen.oneOf(s"created $obRef $value", s"amended $obRef $value", s"cancelled $obRef")
  } yield s"obligation $command"

}
