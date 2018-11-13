package models.behaviors.scheduling.scheduledmessage

import java.time.OffsetDateTime

import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.User
import models.behaviors.events._
import models.behaviors.events.slack.{SlackMessage, SlackMessageEvent}
import models.behaviors.scheduling.Scheduled
import models.behaviors.scheduling.recurrence.Recurrence
import models.team.Team
import services.{DataService, DefaultServices}
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

  def eventFor(channel: String, slackUserId: String, profile: SlackBotProfile, services: DefaultServices)(implicit ec: ExecutionContext): Future[Option[ScheduledMessageSlackEvent]] = {
    Future.successful(
      Some(
        ScheduledMessageSlackEvent(
          SlackMessageEvent(
            SlackEventContext(
              profile,
              channel,
              None,
              slackUserId
            ),
            SlackMessage.fromUnformattedText(text, profile),
            None,
            SlackTimestamp.now,
            Some(EventType.scheduled),
            isUninterruptedConversation = false,
            isEphemeral = false,
            None,
            beQuiet = false
          ),
          this
        )
      )
    )
  }

  def updatedWithNextRunAfter(when: OffsetDateTime): ScheduledMessage = {
    this.copy(nextSentAt = recurrence.nextAfter(when), recurrence = recurrence.incrementTimesHasRun)
  }

  def updateForNextRunAction(dataService: DataService): DBIO[ScheduledMessage] = {
    dataService.scheduledMessages.updateForNextRunAction(this)
  }

  def deleteAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Unit] = {
    dataService.scheduledMessages.deleteAction(this).map { _ =>
      {}
    }
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
