package models.accounts.slack.botprofile

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.user.User
import models.behaviors.behaviorversion.Normal
import models.behaviors.events._
import models.behaviors.events.slack.{SlackMessage, SlackMessageEvent}
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

  def maybeFirstForAction(team: Team, user: User): DBIO[Option[SlackBotProfile]]

  def maybeFirstFor(team: Team, user: User): Future[Option[SlackBotProfile]]

  def allForSlackTeamIdAction(slackTeamId: String): DBIO[Seq[SlackBotProfile]]

  def allForSlackTeamId(slackTeamId: String): Future[Seq[SlackBotProfile]]

  def admin: Future[SlackBotProfile]

  def allSince(when: OffsetDateTime): Future[Seq[SlackBotProfile]]

  def ensure(userId: String, slackTeamId: String, slackTeamName: String, token: String): Future[SlackBotProfile]

  def channelsFor(botProfile: SlackBotProfile): SlackChannels

  def eventualMaybeEvent(slackTeamId: String, channelId: String, maybeUserId: Option[String], maybeOriginalEventType: Option[EventType]): Future[Option[SlackMessageEvent]]

  def eventualMaybeManagedSkillErrorEvent(originalEventType: EventType): Future[Option[SlackMessageEvent]] = {
    eventualMaybeEvent(
      LinkedAccount.ELLIPSIS_SLACK_TEAM_ID,
      LinkedAccount.ELLIPSIS_MANAGED_SKILL_ERRORS_CHANNEL_ID,
      None,
      Some(originalEventType)
    )
  }

  def maybeNameFor(slackTeamId: String): Future[Option[String]]

  def maybeNameForAction(botProfile: SlackBotProfile): DBIO[Option[String]]
  def maybeNameFor(botProfile: SlackBotProfile): Future[Option[String]]

  def toggleMentionShortcut(botProfile: SlackBotProfile, enableShortcut: Boolean): Future[Option[Boolean]]

  def syntheticMessageEvent(
                             botProfile: SlackBotProfile,
                             channelId: String,
                             originalMessageTs: String,
                             maybeThreadTs: Option[String],
                             slackUserId: String,
                             maybeOriginalEventType: Option[EventType],
                             isEphemeral: Boolean,
                             maybeResponseUrl: Option[String],
                             beQuiet: Boolean
                           ): SlackMessageEvent = {
    SlackMessageEvent(
      SlackEventContext(
        botProfile,
        channelId,
        maybeThreadTs,
        slackUserId
      ),
      SlackMessage.blank,
      None,
      maybeThreadTs.orElse(Some(originalMessageTs)),
      maybeOriginalEventType,
      maybeScheduled = None,
      isUninterruptedConversation = false,
      isEphemeral,
      maybeResponseUrl,
      beQuiet
    )
  }

  def sendResultWithNewEvent(
    description: String,
    getEventualMaybeResult: SlackMessageEvent => Future[Option[BotResult]],
    botProfile: SlackBotProfile,
    channelId: String,
    slackUserId: String,
    originalMessageTs: String,
    maybeOriginalEventType: Option[EventType],
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
            profile,
            dmChannel,
            slackUserId,
            OffsetDateTime.now.toString,
            None,
            None,
            isEphemeral = false,
            None,
            beQuiet = false
          )
        }.getOrElse {
          Logger.error(
            s"""Tried to send DM warning message to Slack user ID ${slackUserId}, but I was unable to open a DM channel.
               |
               |Original event info:
               |Ellipsis team ID: ${event.ellipsisTeamId}
               |Slack team ID: ${event.eventContext.teamIdForContext}
               |Original message: $message
             """.stripMargin)
          Future.successful(None)
        }
      } yield maybeTs
    } else {
      Logger.error(s"Bot cannot send DM warning to itself. Original message: $message")
      Future.successful(None)
    }
  }

  private def maybeDmChannelFor(event: Event, services: DefaultServices)
                               (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    event.eventContext match {
      case slackEventContext: SlackEventContext => slackEventContext.eventualMaybeDMChannel(services)
      case _ => {
        Logger.error("Non-Slack event provided to Slack bot profile service while trying to warn a user by DM")
        Future.successful(None)
      }
    }
  }
}
