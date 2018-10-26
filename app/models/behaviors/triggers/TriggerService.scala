package models.behaviors.triggers

import json.BehaviorTriggerData
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait TriggerService {

  def allFor(team: Team): Future[Seq[Trigger]]

  def allActiveFor(team: Team): Future[Seq[Trigger]]

  def allWithExactPattern(pattern: String, teamId: String): Future[Seq[Trigger]]

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[Trigger]]

  def allActiveFor(behaviorGroup: BehaviorGroup): Future[Seq[Trigger]]

  def createForAction(
                       behaviorVersion: BehaviorVersion,
                       pattern: String,
                       requiresBotMention: Boolean,
                       shouldTreatAsRegex: Boolean,
                       isCaseSensitive: Boolean,
                       triggerType: TriggerType
                     ): DBIO[Trigger]

  def createTriggersForAction(
                               behaviorVersion: BehaviorVersion,
                               triggersData: Seq[BehaviorTriggerData]
                             ): DBIO[Seq[Trigger]] = {
    DBIO.sequence(triggersData
      .filterNot(_.text.trim.isEmpty)
      .distinct
      .map { trigger =>
        createForAction(
          behaviorVersion,
          trigger.text,
          trigger.requiresMention,
          trigger.isRegex,
          trigger.caseSensitive,
          TriggerType.definitelyFind(trigger.triggerType)
        )
      }
    )
  }
}
