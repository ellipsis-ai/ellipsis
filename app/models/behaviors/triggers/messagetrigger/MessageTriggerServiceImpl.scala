package models.behaviors.triggers.messagetrigger

import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.triggers.{RegexMessageTrigger, TemplateMessageTrigger}
import models.team.Team
import services.DataService
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroup.BehaviorGroup

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

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

class MessageTriggerServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService]
                                           ) extends MessageTriggerService {

  def dataService = dataServiceProvider.get

  import MessageTriggerQueries._

  def allFor(team: Team): Future[Seq[MessageTrigger]] = {
    val action = allForTeamQuery(team.id).result.map(_.map(tuple2Trigger))
    dataService.run(action)
  }

  def allActiveFor(team: Team): Future[Seq[MessageTrigger]] = {
    val action = allActiveForTeamQuery(team.id).result.map(_.map(tuple2Trigger))
    dataService.run(action)
  }

  def allWithExactPattern(pattern: String, teamId: String): Future[Seq[MessageTrigger]] = {
    val action = allWithBehaviorVersion.
      filter { case(_, (_, ((_, (_, team)), _))) => team.id === teamId }.
      filter { case(trigger, _) => trigger.pattern === pattern }.
      result.
      map(_.map(tuple2Trigger))
    dataService.run(action)
  }

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[MessageTrigger]] = {
    val action = allForBehaviorVersionQuery(behaviorVersion.id).
      result.
      map(_.map(tuple2Trigger))
    dataService.run(action)
  }

  def allActiveFor(behaviorGroup: BehaviorGroup): Future[Seq[MessageTrigger]] = {
    for {
      behaviors <- dataService.behaviors.allForGroup(behaviorGroup)
      versions <- Future.sequence(behaviors.map { ea =>
        dataService.behaviors.maybeCurrentVersionFor(ea)
      }).map(_.flatten)
      triggers <- Future.sequence(versions.map { ea =>
        dataService.messageTriggers.allFor(ea)
      }).map(_.flatten)
    } yield triggers
  }

  val caseInsensitiveRegex: Regex = """\(\?i\)""".r

  def patternWithoutCaseInsensitiveFlag(pattern: String, shouldTreatAsRegex: Boolean): String = {
    if (shouldTreatAsRegex) {
      caseInsensitiveRegex.replaceAllIn(pattern, "")
    } else {
      pattern
    }
  }

  def createForAction(
                       behaviorVersion: BehaviorVersion,
                       pattern: String,
                       requiresBotMention: Boolean,
                       shouldTreatAsRegex: Boolean,
                       isCaseSensitive: Boolean
                     ): DBIO[MessageTrigger] = {
    val processedPattern = patternWithoutCaseInsensitiveFlag(pattern, shouldTreatAsRegex)
    val isCaseSensitiveIntended = isCaseSensitive && (!shouldTreatAsRegex || caseInsensitiveRegex.findFirstMatchIn(pattern).isEmpty)
    val newRaw = RawMessageTrigger(IDs.next, behaviorVersion.id, processedPattern, requiresBotMention, shouldTreatAsRegex, isCaseSensitiveIntended)
    (all += newRaw).map(_ => newRaw).map { _ =>
      val triggerType = if (newRaw.shouldTreatAsRegex) RegexMessageTrigger else TemplateMessageTrigger
      triggerType(newRaw.id, behaviorVersion, newRaw.pattern, newRaw.requiresBotMention, newRaw.isCaseSensitive)
    }
  }

  def allMatching(pattern: String, team: Team): Future[Seq[MessageTrigger]] = {
    for {
      triggers <- allActiveFor(team)
    } yield {
      val regex = ("(?i)" ++ pattern).r
      (triggers).filter { ea =>
        regex.findFirstMatchIn(ea.pattern).isDefined
      }
    }
  }

}
