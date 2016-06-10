package models.bots.triggers

import java.util.regex.PatternSyntaxException

import models.{IDs, Team}
import models.bots._
import services.AWSLambdaConstants
import slick.driver.PostgresDriver.api._
import scala.util.matching.Regex
import scala.concurrent.ExecutionContext.Implicits.global

trait MessageTrigger extends Trigger {

  val pattern: String
  val regex: Regex
  val requiresBotMention: Boolean
  val shouldTreatAsRegex: Boolean
  val isCaseSensitive: Boolean
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

  def matches(fullText: String, relevantMessageText: String, mentionRegex: Regex): Boolean = {
    regex.findFirstMatchIn(relevantMessageText).nonEmpty && (!requiresBotMention || mentionRegex.findFirstMatchIn(fullText).nonEmpty)
  }

  def isActivatedBy(event: Event): Boolean = {
    event match {
      case e: SlackMessageEvent => matches(e.context.message.text, e.context.relevantMessageText, e.context.toBotRegex)
      case _ => false
    }
  }

}

case class RawMessageTrigger(
                              id: String,
                              behaviorId: String,
                              pattern: String,
                              requiresBotMention: Boolean,
                              shouldTreatAsRegex: Boolean,
                              isCaseSensitive: Boolean
                              )

class MessageTriggersTable(tag: Tag) extends Table[RawMessageTrigger](tag, "message_triggers") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorId = column[String]("behavior_id")
  def pattern = column[String]("pattern")
  def requiresBotMention = column[Boolean]("requires_bot_mention")
  def shouldTreatAsRegex = column[Boolean]("treat_as_regex")
  def isCaseSensitive = column[Boolean]("is_case_sensitive")

  def * =
    (id, behaviorId, pattern, requiresBotMention, shouldTreatAsRegex, isCaseSensitive) <> ((RawMessageTrigger.apply _).tupled, RawMessageTrigger.unapply _)
}

object MessageTriggerQueries {

  val all = TableQuery[MessageTriggersTable]
  val allWithBehaviors = all.join(BehaviorQueries.allWithTeam).on(_.behaviorId === _._1.id)

  def tuple2Trigger(tuple: (RawMessageTrigger, (RawBehavior, Team))): MessageTrigger = {
    val raw = tuple._1
    val behavior = BehaviorQueries.tuple2Behavior(tuple._2)
    if (raw.shouldTreatAsRegex) {
      RegexMessageTrigger(raw.id, behavior, raw.pattern.r, raw.requiresBotMention, raw.isCaseSensitive)
    } else {
      TemplateMessageTrigger(raw.id, behavior, raw.pattern, raw.requiresBotMention, raw.isCaseSensitive)
    }
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithBehaviors.filter { case(trigger, (behavior, team)) => team.id === teamId}
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allFor(team: Team): DBIO[Seq[MessageTrigger]] = {
    allForTeamQuery(team.id).result.map(_.map(tuple2Trigger))
  }

  def allWithExactPattern(pattern: String, teamId: String): DBIO[Seq[MessageTrigger]] = {
    allWithBehaviors.
      filter { case(trigger, (behavior, team)) => team.id === teamId }.
      filter { case(trigger, _) => trigger.pattern === pattern }.
      result.
      map(_.map(tuple2Trigger))
  }

  def uncompiledAllForBehaviorQuery(behaviorId: Rep[String]) = {
    allWithBehaviors.filter { case(_, (behavior, _)) => behavior.id === behaviorId}
  }
  val allForBehaviorQuery = Compiled(uncompiledAllForBehaviorQuery _)

  def allFor(behavior: Behavior): DBIO[Seq[MessageTrigger]] = {
    allForBehaviorQuery(behavior.id).
      result.
      map(_.map(tuple2Trigger))
  }

  val caseInsensitiveRegex: Regex = """\(\?i\)""".r

  def patternWithoutCaseInsensitiveFlag(pattern: String, shouldTreatAsRegex: Boolean): String = {
    if (shouldTreatAsRegex) {
      caseInsensitiveRegex.replaceAllIn(pattern, "")
    } else {
      pattern
    }
  }

  def createFor(behavior: Behavior, pattern: String, requiresBotMention: Boolean, shouldTreatAsRegex: Boolean, isCaseSensitive: Boolean): DBIO[MessageTrigger] = {
    val processedPattern = patternWithoutCaseInsensitiveFlag(pattern, shouldTreatAsRegex)
    val isCaseSensitiveIntended = isCaseSensitive && (!shouldTreatAsRegex || caseInsensitiveRegex.findFirstMatchIn(pattern).isEmpty)
    val newRaw = RawMessageTrigger(IDs.next, behavior.id, processedPattern, requiresBotMention, shouldTreatAsRegex, isCaseSensitiveIntended)
    (all += newRaw).map(_ => newRaw).map { _ =>
      if (shouldTreatAsRegex) {
        RegexMessageTrigger(newRaw.id, behavior, newRaw.pattern.r, newRaw.requiresBotMention, newRaw.isCaseSensitive)
      } else {
        TemplateMessageTrigger(newRaw.id, behavior, newRaw.pattern, newRaw.requiresBotMention, newRaw.isCaseSensitive)
      }
    }
  }

  def deleteAllFor(behavior: Behavior): DBIO[Unit] = {
    all.filter(_.behaviorId === behavior.id).delete.map(_ => Unit)
  }

  def allMatching(pattern: String, team: Team): DBIO[Seq[MessageTrigger]] = {
    for {
      triggers <- allFor(team)
    } yield {
      val regex = ("(?i)" ++ pattern).r
      (triggers).filter { ea =>
        regex.findFirstMatchIn(ea.pattern).isDefined
      }
    }
  }

  private def canCompileAsRegex(pattern: String): Boolean = {
    try {
      pattern.r
      true
    } catch {
      case e: PatternSyntaxException => false
    }
  }

}
