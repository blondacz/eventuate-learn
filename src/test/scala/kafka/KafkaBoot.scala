package kafka

import net.manub.embeddedkafka.EmbeddedKafka
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.FreeSpec

import scala.util.Try

class KafkaBoot extends FreeSpec with EmbeddedKafka {

  val commandGen : Gen[String] = for {
    obRef <- Gen.alphaLowerChar
    value <- Gen.chooseNum(100,200)
    command <- Gen.oneOf(s"created $obRef $value",s"amended $obRef $value",s"cancelled $obRef")
  } yield s"obligation $command"

  "runs kafka until killed" in {
    EmbeddedKafka.start()
    var i = 1
    while (true) {

      publishStringMessageToKafka("obligation-events", commandGen.sample.get)
      i += 1
      Thread.sleep(1000)
      val msg = Try(consumeFirstStringMessageFrom("obligation-events"))
      println(s"Received: $msg")
    }

    EmbeddedKafka.stop()
  }

}
