package models.bots.triggers

import models.bots._
import models.{IDs, Team}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex

case class RegexMessageTrigger(
                                id: String,
                                behavior: Behavior,
                                regex: Regex
                                ) extends MessageTrigger {

  val sortRank: Int = 2

  val pattern: String = regex.pattern.pattern()

  protected def paramIndexMaybesFor(params: Seq[BehaviorParameter]): Seq[Option[Int]] = {
    0.to(regex.pattern.matcher("").groupCount()).map { i =>
      Some(i)
    }
  }
}
