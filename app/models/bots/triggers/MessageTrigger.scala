package models.bots.triggers

import java.util.regex.PatternSyntaxException

import models.Team
import models.bots.{Behavior, BehaviorParameter, SlackMessageEvent, Event}
import services.AWSLambdaConstants
import slick.driver.PostgresDriver.api._
import scala.util.matching.Regex
import scala.concurrent.ExecutionContext.Implicits.global

trait MessageTrigger extends Trigger {

  val pattern: String
  val regex: Regex
  val sortRank: Int

  protected def paramIndexMaybesFor(params: Seq[BehaviorParameter]): Seq[Option[Int]]

  def invocationParamsFor(message: String, params: Seq[BehaviorParameter]): Map[String, String] = {
    regex.findFirstMatchIn(message).map { firstMatch =>
      firstMatch.subgroups.zip(paramIndexMaybesFor(params)).flatMap { case(paramValue, maybeRank) =>
        maybeRank.map { rank =>
          (AWSLambdaConstants.invocationParamFor(rank), paramValue)
        }
      }.toMap
    }.getOrElse(Map())
  }

  def invocationParamsFor(event: Event, params: Seq[BehaviorParameter]): Map[String, String] = {
    event match {
      case e: SlackMessageEvent => invocationParamsFor(e.context.relevantMessageText, params)
      case _ => Map()
    }
  }

  def matches(text: String): Boolean = regex.findFirstMatchIn(text).nonEmpty

  def isActivatedBy(event: Event): Boolean = {
    event match {
      case e: SlackMessageEvent => matches(e.context.relevantMessageText)
      case _ => false
    }
  }

}

object MessageTriggerQueries {

  def allFor(team: Team): DBIO[Seq[MessageTrigger]] = {
    for {
      regexTriggers <- RegexMessageTriggerQueries.allFor(team)
      templateTriggers <- TemplateMessageTriggerQueries.allFor(team)
    } yield regexTriggers ++ templateTriggers
  }

  def allFor(behavior: Behavior): DBIO[Seq[MessageTrigger]] = {
    for {
      regexTriggers <- RegexMessageTriggerQueries.allFor(behavior)
      templateTriggers <- TemplateMessageTriggerQueries.allFor(behavior)
    } yield {
      (regexTriggers ++ templateTriggers).sortBy(ea => (ea.sortRank, ea.pattern))
    }
  }

  def allMatching(pattern: String, team: Team): DBIO[Seq[MessageTrigger]] = {
    for {
      regexTriggers <- RegexMessageTriggerQueries.allFor(team)
      templateTriggers <- TemplateMessageTriggerQueries.allFor(team)
    } yield {
      val regex = ("(?i)" ++ pattern).r
      (regexTriggers ++ templateTriggers).filter { ea =>
        regex.findFirstMatchIn(ea.pattern).isDefined
      }
    }
  }

  def allWithExactPattern(pattern: String, teamId: String): DBIO[Seq[MessageTrigger]] = {
    for {
      regexTriggers <- RegexMessageTriggerQueries.allWithExactPattern(pattern, teamId)
      templateTriggers <- TemplateMessageTriggerQueries.allWithExactPattern(pattern, teamId)
    } yield regexTriggers ++ templateTriggers
  }

  private def canCompileAsRegex(pattern: String): Boolean = {
    try {
      pattern.r
      true
    } catch {
      case e: PatternSyntaxException => false
    }
  }

  // ¯\_(ツ)_/¯
  private def looksLikeRegex(pattern: String): Boolean = {
    canCompileAsRegex(pattern) && pattern.contains("""\s""")
  }

  def ensureFor(behavior: Behavior, pattern: String): DBIO[MessageTrigger] = {
    if (looksLikeRegex(pattern)) {
      RegexMessageTriggerQueries.ensureFor(behavior, pattern.r)
    } else {
      TemplateMessageTriggerQueries.ensureFor(behavior, pattern)
    }
  }

  def deleteAllFor(behavior: Behavior): DBIO[Unit] = {
    for {
      _ <- RegexMessageTriggerQueries.deleteAllFor(behavior)
      _ <- TemplateMessageTriggerQueries.deleteAllFor(behavior)
    } yield Unit
  }

}
