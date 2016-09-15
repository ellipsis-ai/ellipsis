package models.behaviors.triggers.messagetrigger

import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team

import scala.concurrent.Future

trait MessageTriggerService {

  def allFor(team: Team): Future[Seq[MessageTrigger]]

  def allActiveFor(team: Team): Future[Seq[MessageTrigger]]

  def allWithExactPattern(pattern: String, teamId: String): Future[Seq[MessageTrigger]]

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[MessageTrigger]]

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 pattern: String,
                 requiresBotMention: Boolean,
                 shouldTreatAsRegex: Boolean,
                 isCaseSensitive: Boolean
               ): Future[MessageTrigger]

  def allMatching(pattern: String, team: Team): Future[Seq[MessageTrigger]]

}
