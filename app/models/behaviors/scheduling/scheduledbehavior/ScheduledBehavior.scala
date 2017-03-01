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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ScheduledBehavior(
                              id: String,
                              behavior: Behavior,
                              arguments: Map[String, String],
                              maybeUser: Option[User],
                              team: Team,
                              maybeChannelName: Option[String],
                              isForIndividualMembers: Boolean,
                              recurrence: Recurrence,
                              nextSentAt: OffsetDateTime,
                              createdAt: OffsetDateTime
                           ) extends Scheduled {

  def displayText(dataService: DataService): Future[String] = {
    dataService.behaviors.maybeCurrentVersionFor(behavior).map { maybeVersion =>
      maybeVersion.map { version =>
        val actionText = version.maybeName.map { name =>
          s"""an action named `${name}`"""
        }.getOrElse("an unnamed action")
        val groupText = (for {
          group <- behavior.maybeGroup
          groupName <- Option(group.name).filter(_.trim.nonEmpty)
        } yield {
          s" in skill `$groupName`"
        }).getOrElse("")
        s"$actionText$groupText"
      }.getOrElse("an unnamed action")
    }
  }

  def eventFor(channelName: String, slackUserId: String, profile: SlackBotProfile): ScheduledEvent = {
    ScheduledEvent(RunEvent(profile, behavior, arguments, channelName, None, slackUserId, "ts"), this)
  }

  def withUpdatedNextTriggeredFor(when: OffsetDateTime): ScheduledBehavior = {
    this.copy(nextSentAt = recurrence.nextAfter(when))
  }

  def updateNextTriggeredFor(dataService: DataService): Future[ScheduledBehavior] = {
    dataService.scheduledBehaviors.updateNextTriggeredFor(this)
  }

  def toRaw: RawScheduledBehavior = {
    RawScheduledBehavior(
      id,
      behavior.id,
      Json.toJson(arguments),
      maybeUser.map(_.id),
      team.id,
      maybeChannelName,
      isForIndividualMembers,
      recurrence.id,
      nextSentAt,
      createdAt
    )
  }
}
