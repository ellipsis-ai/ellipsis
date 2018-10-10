package models.accounts.slack.botprofile

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import models.behaviors.behaviorversion.Normal
import models.behaviors.events._
import models.behaviors.{BotResult, SimpleTextResult}
import models.team.Team
import play.api.Logger
import services.DefaultServices
import slick.dbio.DBIO
import utils.SlackChannels

import scala.concurrent.{ExecutionContext, Future}

trait SlackBotProfileService {

  def allProfiles: Future[Seq[SlackBotProfile]]

  def allForAction(team: Team): DBIO[Seq[SlackBotProfile]]

  def allFor(team: Team): Future[Seq[SlackBotProfile]]

  def allForSlackTeamId(slackTeamId: String): Future[Seq[SlackBotProfile]]

  def admin: Future[SlackBotProfile]

  def allSince(when: OffsetDateTime): Future[Seq[SlackBotProfile]]

  def ensure(userId: String, maybeEnterpriseId: Option[String], slackTeamId: String, slackTeamName: String, token: String): Future[SlackBotProfile]

  def channelsFor(botProfile: SlackBotProfile): SlackChannels

  def eventualMaybeEvent(slackTeamId: String, channelId: String, maybeUserId: Option[String], maybeOriginalEventType: Option[EventType]): Future[Option[SlackMessageEvent]]

  def maybeNameFor(slackTeamId: String): Future[Option[String]]

  def maybeNameFor(botProfile: SlackBotProfile): Future[Option[String]]

  def toggleMentionShortcut(botProfile: SlackBotProfile, enableShortcut: Boolean): Future[Option[Boolean]]

  def sendResultWithNewEvent(
    description: String,
    getEventualMaybeResult: SlackMessageEvent => Future[Option[BotResult]],
    slackTeamId: String,
    botProfile: SlackBotProfile,
    channelId: String,
    userId: String,
    originalMessageTs: String,
    maybeThreadTs: Option[String],
    isEphemeral: Boolean,
    maybeResponseUrl: Option[String],
    beQuiet: Boolean
  ): Future[Option[String]]

  def sendDMWarningMessageFor(event: Event, services: DefaultServices, profile: SlackBotProfile, slackUserId: String, message: String)
                             (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    if (slackUserId != profile.userId) {
      val wholeMessage =
        s"""---
           |
           |**Warning:** $message
           |
           |---
           |
         """.stripMargin
      for {
        maybeDmChannel <- maybeDmChannelFor(event, services)
        maybeTs <- maybeDmChannel.map { dmChannel =>
          sendResultWithNewEvent(
            "Warning message to user via DM",
            (newEvent) => Future.successful(Some(SimpleTextResult(newEvent, None, wholeMessage, Normal, shouldInterrupt = false))),
            profile.slackTeamId,
            profile,
            dmChannel,
            slackUserId,
            OffsetDateTime.now.toString,
            None,
            isEphemeral = false,
            None,
            beQuiet = false
          )
        }.getOrElse(Future.successful(None))
      } yield maybeTs
    } else {
      Logger.error(s"Bot cannot send DM warning to itself. Original message: $message")
      Future.successful(None)
    }
  }

  private def maybeDmChannelFor(event: Event, services: DefaultServices)
                               (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    event match {
      case slackEvent: SlackEvent => slackEvent.eventualMaybeDMChannel(services)
      case scheduledEvent: ScheduledEvent => maybeDmChannelFor(scheduledEvent.underlying, services)
      case _ => {
        Logger.error("Non-Slack event provided to Slack bot profile service while trying to warn a user by DM")
        Future.successful(None)
      }
    }
  }
}
