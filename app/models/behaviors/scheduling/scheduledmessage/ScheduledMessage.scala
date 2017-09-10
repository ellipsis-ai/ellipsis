package models.behaviors.scheduling.scheduledmessage

import java.time.OffsetDateTime

import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.events.{ScheduledEvent, SlackMessage, SlackMessageEvent}
import models.behaviors.scheduling.Scheduled
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team
import services.DataService
import slack.api.SlackApiClient
import slick.dbio.DBIO
import utils.SlackTimestamp

import scala.concurrent.{ExecutionContext, Future}

case class ScheduledMessage(
                             id: String,
                             text: String,
                             maybeUser: Option[User],
                             team: Team,
                             maybeChannel: Option[String],
                             isForIndividualMembers: Boolean,
                             recurrence: Recurrence,
                             nextSentAt: OffsetDateTime,
                             createdAt: OffsetDateTime
                           ) extends Scheduled {

  def displayText(dataService: DataService)(implicit ec: ExecutionContext): Future[String] = {
    Future.successful(s"`$text`")
  }

  def eventFor(channel: String, slackUserId: String, profile: SlackBotProfile, client: SlackApiClient): ScheduledEvent = {
    ScheduledEvent(SlackMessageEvent(profile, channel, None, slackUserId, SlackMessage.fromUnformattedText(text, profile.userId), SlackTimestamp.now, client), this)
  }

  def withUpdatedNextTriggeredFor(when: OffsetDateTime): ScheduledMessage = {
    this.copy(nextSentAt = recurrence.nextAfter(when))
  }

  def updateNextTriggeredForAction(dataService: DataService): DBIO[ScheduledMessage] = {
    dataService.scheduledMessages.updateNextTriggeredForAction(this)
  }

  def toRaw: RawScheduledMessage = {
    RawScheduledMessage(
      id,
      text,
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

object ScheduledMessage {
  val tableName: String = "scheduled_messages"
}
