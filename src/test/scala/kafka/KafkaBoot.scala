package kafka

import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.FreeSpec

import scala.util.Try

class KafkaBoot extends FreeSpec with EmbeddedKafka {

  "runs kafka until killed" in {
    EmbeddedKafka.start()
    var i = 1
    while (true) {

      publishStringMessageToKafka("obligation-events", s"obligation created ab$i 13.00")
      i += 1
      Thread.sleep(1000)
      val msg = Try(consumeFirstStringMessageFrom("obligation-events"))
      println(s"Received: $msg")
    }

    EmbeddedKafka.stop()
  }

}
