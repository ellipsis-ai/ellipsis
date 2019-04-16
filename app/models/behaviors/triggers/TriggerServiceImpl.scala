package models.behaviors.triggers

import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import services.DataService
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroup.BehaviorGroup

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

case class RawTrigger(
                              id: String,
                              behaviorVersionId: String,
                              triggerType: TriggerType,
                              pattern: String,
                              requiresBotMention: Boolean,
                              shouldTreatAsRegex: Boolean,
                              isCaseSensitive: Boolean
                            )

class TriggersTable(tag: Tag) extends Table[RawTrigger](tag, "message_triggers") {

  implicit val triggerTypeColumnType = MappedColumnType.base[TriggerType, String](
    { gt => gt.toString },
    { str => TriggerType.definitelyFind(str) }
  )

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def triggerType = column[TriggerType]("trigger_type")
  def pattern = column[String]("pattern")
  def requiresBotMention = column[Boolean]("requires_bot_mention")
  def shouldTreatAsRegex = column[Boolean]("treat_as_regex")
  def isCaseSensitive = column[Boolean]("is_case_sensitive")

  def * =
    (id, behaviorVersionId, triggerType, pattern, requiresBotMention, shouldTreatAsRegex, isCaseSensitive) <> ((RawTrigger.apply _).tupled, RawTrigger.unapply _)
}

class TriggerServiceImpl @Inject()(
                                             dataServiceProvider: Provider[DataService],
                                             implicit val ec: ExecutionContext
                                           ) extends TriggerService {

  def dataService = dataServiceProvider.get

  import TriggerQueries._

  def allFor(team: Team): Future[Seq[Trigger]] = {
    val action = allForTeamQuery(team.id).result.map(_.map(tuple2Trigger))
    dataService.run(action)
  }

  def allActiveFor(team: Team): Future[Seq[Trigger]] = {
    val action = allActiveForTeamQuery(team.id).result.map(_.map(tuple2Trigger))
    dataService.run(action)
  }

  def allWithExactPattern(pattern: String, teamId: String): Future[Seq[Trigger]] = {
    val action = allWithBehaviorVersion.
      filter { case(_, (_, ((_, (_, team)), _))) => team.id === teamId }.
      filter { case(trigger, _) => trigger.pattern === pattern }.
      result.
      map(_.map(tuple2Trigger))
    dataService.run(action)
  }

  def allForAction(behaviorVersion: BehaviorVersion): DBIO[Seq[Trigger]] = {
    allForBehaviorVersionQuery(behaviorVersion.id).
      result.
      map(_.map(tuple2Trigger))
  }

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[Trigger]] = {
    dataService.run(allForAction(behaviorVersion))
  }

  def allActiveFor(behaviorGroup: BehaviorGroup): Future[Seq[Trigger]] = {
    for {
      behaviors <- dataService.behaviors.allForGroup(behaviorGroup)
      versions <- Future.sequence(behaviors.map { ea =>
        dataService.behaviors.maybeCurrentVersionFor(ea)
      }).map(_.flatten)
      triggers <- Future.sequence(versions.map { ea =>
        dataService.triggers.allFor(ea)
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

  protected def createForAction(
                       behaviorVersion: BehaviorVersion,
                       pattern: String,
                       requiresBotMention: Boolean,
                       shouldTreatAsRegex: Boolean,
                       isCaseSensitive: Boolean,
                       triggerType: TriggerType
                     ): DBIO[Trigger] = {
    val processedPattern = patternWithoutCaseInsensitiveFlag(pattern, shouldTreatAsRegex)
    val isCaseSensitiveIntended = isCaseSensitive && (!shouldTreatAsRegex || caseInsensitiveRegex.findFirstMatchIn(pattern).isEmpty)
    val newRaw = RawTrigger(IDs.next, behaviorVersion.id, triggerType, processedPattern, requiresBotMention, shouldTreatAsRegex, isCaseSensitiveIntended)
    (all += newRaw).map(_ => newRaw).map { _ =>
      val triggerKind = if (newRaw.shouldTreatAsRegex) RegexTrigger else TemplateTrigger
      triggerKind(newRaw.id, behaviorVersion, newRaw.triggerType, newRaw.pattern, newRaw.requiresBotMention, newRaw.isCaseSensitive)
    }
  }

}
