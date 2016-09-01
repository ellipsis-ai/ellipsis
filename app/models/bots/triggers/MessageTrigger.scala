package models.bots.triggers

import java.util.regex.PatternSyntaxException

import models.accounts.user.User
import models.{IDs, Team}
import models.bots._
import models.bots.events.{Event, MessageEvent}
import services.AWSLambdaConstants
import slick.driver.PostgresDriver.api._

import scala.util.matching.Regex
import scala.concurrent.ExecutionContext.Implicits.global

trait MessageTrigger extends Trigger {

  val pattern: String
  def regex: Regex
  val requiresBotMention: Boolean
  val shouldTreatAsRegex: Boolean
  val isCaseSensitive: Boolean
  val sortRank: Int

  protected def paramIndexMaybesFor(params: Seq[BehaviorParameter]): Seq[Option[Int]]

  def isValidRegex: Boolean = {
    try {
      regex
      true
    } catch {
      case e: PatternSyntaxException => false
    }
  }

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
      case e: MessageEvent => invocationParamsFor(e.context.relevantMessageText, params)
      case _ => Map()
    }
  }

  def matches(relevantMessageText: String, includesBotMention: Boolean): Boolean = {
    isValidRegex && regex.findFirstMatchIn(relevantMessageText).nonEmpty && (!requiresBotMention || includesBotMention)
  }

  def isActivatedBy(event: Event): Boolean = {
    event match {
      case e: MessageEvent => matches(e.context.relevantMessageText, e.context.includesBotMention)
      case _ => false
    }
  }

}

case class RawMessageTrigger(
                              id: String,
                              behaviorVersionId: String,
                              pattern: String,
                              requiresBotMention: Boolean,
                              shouldTreatAsRegex: Boolean,
                              isCaseSensitive: Boolean
                              )

class MessageTriggersTable(tag: Tag) extends Table[RawMessageTrigger](tag, "message_triggers") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def pattern = column[String]("pattern")
  def requiresBotMention = column[Boolean]("requires_bot_mention")
  def shouldTreatAsRegex = column[Boolean]("treat_as_regex")
  def isCaseSensitive = column[Boolean]("is_case_sensitive")

  def * =
    (id, behaviorVersionId, pattern, requiresBotMention, shouldTreatAsRegex, isCaseSensitive) <> ((RawMessageTrigger.apply _).tupled, RawMessageTrigger.unapply _)
}

object MessageTriggerQueries {

  val all = TableQuery[MessageTriggersTable]
  val allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithBehavior).on(_.behaviorVersionId === _._1._1.id)

  type TupleType = (RawMessageTrigger, ((RawBehaviorVersion, Option[User]), (RawBehavior, Team)))

  def tuple2Trigger(tuple: TupleType): MessageTrigger = {
    val raw = tuple._1
    val behaviorVersion = BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2)
    val triggerType = if (raw.shouldTreatAsRegex) RegexMessageTrigger else TemplateMessageTrigger
    triggerType(raw.id, behaviorVersion, raw.pattern, raw.requiresBotMention, raw.isCaseSensitive)
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithBehaviorVersion.filter { case(trigger, (behaviorVersion, (behavior, team))) => team.id === teamId}
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allFor(team: Team): DBIO[Seq[MessageTrigger]] = {
    allForTeamQuery(team.id).result.map(_.map(tuple2Trigger))
  }

  def uncompiledAllActiveForTeamQuery(teamId: Rep[String]) = {
    allWithBehaviorVersion.
      filter { case(_, (_, (_, team))) => team.id === teamId}.
      filter { case(_, ((behaviorVersion, _), (behavior, team))) => behaviorVersion.id === behavior.maybeCurrentVersionId }
  }
  val allActiveForTeamQuery = Compiled(uncompiledAllActiveForTeamQuery _)

  def allActiveFor(team: Team): DBIO[Seq[MessageTrigger]] = {
    allActiveForTeamQuery(team.id).result.map(_.map(tuple2Trigger))
  }

  def allWithExactPattern(pattern: String, teamId: String): DBIO[Seq[MessageTrigger]] = {
    allWithBehaviorVersion.
      filter { case(trigger, (behaviorVersion, (behavior, team))) => team.id === teamId }.
      filter { case(trigger, _) => trigger.pattern === pattern }.
      result.
      map(_.map(tuple2Trigger))
  }

  def uncompiledAllForBehaviorQuery(behaviorVersionId: Rep[String]) = {
    allWithBehaviorVersion.filter { case(_, ((behaviorVersion, _), _)) => behaviorVersion.id === behaviorVersionId}
  }
  val allForBehaviorQuery = Compiled(uncompiledAllForBehaviorQuery _)

  def allFor(behaviorVersion: BehaviorVersion): DBIO[Seq[MessageTrigger]] = {
    allForBehaviorQuery(behaviorVersion.id).
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

  def createFor(behaviorVersion: BehaviorVersion, pattern: String, requiresBotMention: Boolean, shouldTreatAsRegex: Boolean, isCaseSensitive: Boolean): DBIO[MessageTrigger] = {
    val processedPattern = patternWithoutCaseInsensitiveFlag(pattern, shouldTreatAsRegex)
    val isCaseSensitiveIntended = isCaseSensitive && (!shouldTreatAsRegex || caseInsensitiveRegex.findFirstMatchIn(pattern).isEmpty)
    val newRaw = RawMessageTrigger(IDs.next, behaviorVersion.id, processedPattern, requiresBotMention, shouldTreatAsRegex, isCaseSensitiveIntended)
    (all += newRaw).map(_ => newRaw).map { _ =>
      val triggerType = if (newRaw.shouldTreatAsRegex) RegexMessageTrigger else TemplateMessageTrigger
      triggerType(newRaw.id, behaviorVersion, newRaw.pattern, newRaw.requiresBotMention, newRaw.isCaseSensitive)
    }
  }

  def deleteAllFor(behaviorVersion: BehaviorVersion): DBIO[Unit] = {
    all.filter(_.behaviorVersionId === behaviorVersion.id).delete.map(_ => Unit)
  }

  def allMatching(pattern: String, team: Team): DBIO[Seq[MessageTrigger]] = {
    for {
      triggers <- allActiveFor(team)
    } yield {
      val regex = ("(?i)" ++ pattern).r
      (triggers).filter { ea =>
        regex.findFirstMatchIn(ea.pattern).isDefined
      }
    }
  }

  def maybeRegexValidationErrorFor(pattern: String): Option[String] = {
    try {
      pattern.r
      None
    } catch {
      case e: PatternSyntaxException => Some(e.getMessage)
    }
  }

}
