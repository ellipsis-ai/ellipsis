package models.bots.triggers.messagetrigger

import models.accounts.user.User
import models.bots.behavior.RawBehavior
import models.bots.behaviorversion.{BehaviorVersionQueries, RawBehaviorVersion}
import models.bots.triggers.{RegexMessageTrigger, TemplateMessageTrigger}
import models.team.Team
import slick.driver.PostgresDriver.api._

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

  def uncompiledAllActiveForTeamQuery(teamId: Rep[String]) = {
    allWithBehaviorVersion.
      filter { case(_, (_, (_, team))) => team.id === teamId}.
      filter { case(_, ((behaviorVersion, _), (behavior, team))) => behaviorVersion.id === behavior.maybeCurrentVersionId }
  }
  val allActiveForTeamQuery = Compiled(uncompiledAllActiveForTeamQuery _)

  def uncompiledAllForBehaviorQuery(behaviorVersionId: Rep[String]) = {
    allWithBehaviorVersion.filter { case(_, ((behaviorVersion, _), _)) => behaviorVersion.id === behaviorVersionId}
  }
  val allForBehaviorQuery = Compiled(uncompiledAllForBehaviorQuery _)

}
