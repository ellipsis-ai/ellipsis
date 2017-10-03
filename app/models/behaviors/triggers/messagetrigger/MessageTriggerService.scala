package models.behaviors.triggers.messagetrigger

import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait MessageTriggerService {

  def allActiveFor(team: Team): Future[Seq[MessageTrigger]]

  def allWithExactPattern(pattern: String, teamId: String): Future[Seq[MessageTrigger]]

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[MessageTrigger]]

  def allActiveFor(behaviorGroup: BehaviorGroup): Future[Seq[MessageTrigger]]

  def allBuiltin: Future[Seq[MessageTrigger]]

  def createForAction(
                       behaviorVersion: BehaviorVersion,
                       pattern: String,
                       requiresBotMention: Boolean,
                       shouldTreatAsRegex: Boolean,
                       isCaseSensitive: Boolean
                     ): DBIO[MessageTrigger]

}
