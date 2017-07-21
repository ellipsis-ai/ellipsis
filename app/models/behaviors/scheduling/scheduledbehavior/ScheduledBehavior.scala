package models.behaviors.scheduling.scheduledbehavior

import java.time.OffsetDateTime

import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.events.{RunEvent, ScheduledEvent}
import models.behaviors.scheduling.Scheduled
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team
import play.api.libs.json.Json
import services.DataService
import slick.dbio.DBIO
import utils.SlackTimestamp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ScheduledBehavior(
                              id: String,
                              behavior: Behavior,
                              arguments: Map[String, String],
                              maybeUser: Option[User],
                              team: Team,
                              maybeChannel: Option[String],
                              isForIndividualMembers: Boolean,
                              recurrence: Recurrence,
                              nextSentAt: OffsetDateTime,
                              createdAt: OffsetDateTime
                           ) extends Scheduled {

  def maybeBehaviorName(dataService: DataService): Future[Option[String]] = {
    for {
      maybeBehaviorVersion <- dataService.behaviors.maybeCurrentVersionFor(behavior)
    } yield {
      maybeBehaviorVersion.flatMap(_.maybeName)
    }
  }

  def maybeBehaviorGroupName(dataService: DataService): Future[Option[String]] = {
    for {
      maybeGroupVersion <- dataService.behaviorGroups.maybeCurrentVersionFor(behavior.group)
    } yield {
      maybeGroupVersion.flatMap(version => Option(version.name).filter(_.trim.nonEmpty))
    }
  }

  def displayText(dataService: DataService): Future[String] = {
    for {
      maybeBehaviorName <- maybeBehaviorName(dataService)
      maybeBehaviorGroupName <- maybeBehaviorGroupName(dataService)
    } yield {
      val actionText = maybeBehaviorName.map { name =>
        s"""an action named `${name}`"""
      }.getOrElse("an unnamed action")
      val groupText = maybeBehaviorGroupName.map { name =>
        s" in skill `$name`"
      }.getOrElse("")
      actionText ++ groupText
    }
  }

  def eventFor(channel: String, slackUserId: String, profile: SlackBotProfile): ScheduledEvent = {
    ScheduledEvent(RunEvent(profile, behavior, arguments, channel, None, slackUserId, SlackTimestamp.now), this)
  }

  def withUpdatedNextTriggeredFor(when: OffsetDateTime): ScheduledBehavior = {
    this.copy(nextSentAt = recurrence.nextAfter(when))
  }

  def updateNextTriggeredForAction(dataService: DataService): DBIO[ScheduledBehavior] = {
    dataService.scheduledBehaviors.updateNextTriggeredForAction(this)
  }

  def toRaw: RawScheduledBehavior = {
    RawScheduledBehavior(
      id,
      behavior.id,
      Json.toJson(arguments),
      maybeUser.map(_.id),
      team.id,
      maybeChannel,
      isForIndividualMembers,
      recurrence.id,
      nextSentAt,
      createdAt
    )
  }
}

object ScheduledBehavior {
  val tableName: String = "scheduled_behaviors"
}
