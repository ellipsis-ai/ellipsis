package models.behaviors.triggers.messagetrigger

import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorversion.BehaviorVersionQueries
import models.behaviors.triggers.{RegexMessageTrigger, TemplateMessageTrigger}
import models.team.TeamsTable

object MessageTriggerQueries {

  val all = TableQuery[MessageTriggersTable]
  val allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithGroupVersion).on(_.behaviorVersionId === _._1._1._1.id)

  type TupleType = (RawMessageTrigger, BehaviorVersionQueries.TupleType)
  type TableTupleType = (MessageTriggersTable, BehaviorVersionQueries.TableTupleType)

  def tuple2Trigger(tuple: TupleType): MessageTrigger = {
    val raw = tuple._1
    val behaviorVersion = BehaviorVersionQueries.tuple2BehaviorVersion(tuple._2)
    val triggerType = if (raw.shouldTreatAsRegex) RegexMessageTrigger else TemplateMessageTrigger
    triggerType(raw.id, behaviorVersion, raw.pattern, raw.requiresBotMention, raw.isCaseSensitive)
  }

  def teamFor(tuple: TableTupleType): TeamsTable = tuple._2._1._2._1._2

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithBehaviorVersion.filter { case(trigger, (behaviorVersion, ((behavior, (_, team)), _))) => team.id === teamId}
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def uncompiledAllActiveForTeamQuery(teamId: Rep[String]) = {
    allWithBehaviorVersion.
      filter { case(_, (_, ((_, (_, team)), _))) => team.id === teamId }.
      filter { case(_, (_, ((groupVersion, (group, _)), _))) => group.maybeCurrentVersionId === groupVersion.id }
  }
  val allActiveForTeamQuery = Compiled(uncompiledAllActiveForTeamQuery _)

  def uncompiledAllForBehaviorVersionQuery(behaviorVersionId: Rep[String]) = {
    allWithBehaviorVersion.filter { case(_, (((behaviorVersion, _), _), _)) => behaviorVersion.id === behaviorVersionId}
  }
  val allForBehaviorVersionQuery = Compiled(uncompiledAllForBehaviorVersionQuery _)

}
