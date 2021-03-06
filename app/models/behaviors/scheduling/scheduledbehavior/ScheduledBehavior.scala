package models.behaviors.scheduling.scheduledbehavior

import java.time.OffsetDateTime

import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.slack.SlackRunEvent
import models.behaviors.events.{EventType, SlackEventContext}
import models.behaviors.scheduling.Scheduled
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team
import play.api.libs.json.Json
import services.{DataService, DefaultServices}
import slick.dbio.DBIO

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

  val maybeBehaviorGroupId: Option[String] = behavior.maybeGroup.map(_.id)

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

  def maybeBehaviorVersionFor(channel: String, services: DefaultServices)(implicit ec: ExecutionContext): Future[Option[BehaviorVersion]] = {
    for {
      maybeGroupVersion <- services.dataService.behaviorGroupDeployments.maybeActiveBehaviorGroupVersionFor(behavior.group, context.name, channel)
      maybeBehaviorVersion <- maybeGroupVersion.map { groupVersion =>
        services.dataService.behaviorVersions.findFor(behavior, groupVersion)
      }.getOrElse(Future.successful(None))
    } yield maybeBehaviorVersion
  }

  def eventFor(channel: String, slackUserId: String, profile: SlackBotProfile, services: DefaultServices)(implicit ec: ExecutionContext): Future[Option[SlackRunEvent]] = {
    maybeBehaviorVersionFor(channel, services).map { maybeBehaviorVersion =>
      maybeBehaviorVersion.map { behaviorVersion =>
        SlackRunEvent(
          SlackEventContext(
            profile,
            channel,
            None,
            slackUserId
          ),
          behaviorVersion,
          arguments,
          EventType.scheduled,
          Some(EventType.scheduled),
          maybeScheduled = Some(this),
          isEphemeral = false,
          None,
          None
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
