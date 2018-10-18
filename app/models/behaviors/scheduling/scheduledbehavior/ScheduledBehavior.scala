package models.behaviors.scheduling.scheduledbehavior

import java.time.OffsetDateTime

import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.events.{EventType, RunEvent, ScheduledEvent}
import models.behaviors.scheduling.Scheduled
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team
import play.api.libs.json.Json
import services.{DataService, DefaultServices}
import slick.dbio.DBIO
import utils.SlackTimestamp

import scala.concurrent.{ExecutionContext, Future}

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

  def displayText(dataService: DataService)(implicit ec: ExecutionContext): Future[String] = {
    for {
      maybeBehaviorVersion <- dataService.behaviors.maybeCurrentVersionFor(behavior)
      maybeBehaviorGroupVersion <- dataService.behaviorGroups.maybeCurrentVersionFor(behavior.group)
    } yield {
      val actionText = maybeBehaviorVersion.map { behaviorVersion =>
        behaviorVersion.maybeName.map { name =>
          s"""an action named `$name`"""
        }.getOrElse("an unnamed action")
      }.getOrElse("a deleted action")
      val groupText = maybeBehaviorGroupVersion.map { groupVersion =>
        if (groupVersion.name.trim.nonEmpty) {
          s" in skill `${groupVersion.name}`"
        } else {
          s" in an unnamed skill"
        }
      }.getOrElse(" in a deleted skill")
      actionText ++ groupText
    }
  }

  def eventFor(channel: String, slackUserId: String, profile: SlackBotProfile, services: DefaultServices)(implicit ec: ExecutionContext): Future[Option[ScheduledEvent]] = {
    services.dataService.behaviors.maybeCurrentVersionFor(behavior).map { maybeBehaviorVersion =>
      maybeBehaviorVersion.map { behaviorVersion =>
        ScheduledEvent(
          RunEvent(
            profile,
            behaviorVersion,
            arguments,
            channel,
            None,
            slackUserId,
            SlackTimestamp.now,
            Some(EventType.scheduled),
            isEphemeral = false,
            None
          ),
          this
        )
      }
    }
  }

  def updatedWithNextRunAfter(when: OffsetDateTime): ScheduledBehavior = {
    this.copy(nextSentAt = recurrence.nextAfter(when), recurrence = recurrence.incrementTimesHasRun)
  }

  def updateForNextRunAction(dataService: DataService): DBIO[ScheduledBehavior] = {
    dataService.scheduledBehaviors.updateForNextRunAction(this)
  }

  def deleteAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Unit] = {
    dataService.scheduledBehaviors.deleteAction(this).map { _ =>
      {}
    }
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
