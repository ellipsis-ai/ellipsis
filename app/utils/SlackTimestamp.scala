package utils

import java.time.OffsetDateTime

import scala.util.Random

object SlackTimestamp {

  val random = new Random()

  def randomPart: String = random.nextInt(999999).toString

  def now: String = {
    OffsetDateTime.now.toInstant.toEpochMilli.toString ++ "." ++ randomPart
  }
}
