package mocks

import akka.actor.ActorSystem
import json.{SlackUserData, SlackUserProfileData}
import models.accounts.slack.SlackUserTeamIds
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.Event
import models.behaviors.events.slack.SlackMessage
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import services.slack.apiModels.{Attachment, SlackUser}
import services.slack.{SlackApiClient, SlackApiError, SlackEventService}
import slick.dbio.DBIO

import scala.concurrent.Future

class MockSlackEventService extends SlackEventService with MockitoSugar {

  val client: SlackApiClient = {
    val client = mock[SlackApiClient]
    implicit val system = ActorSystem("slack")
    when(client.listConversations()).thenReturn(Future.successful(Seq()))
    when(client.postChatMessage(
      anyString,
      anyString,
      any[Option[String]],
      any[Option[Boolean]],
      any[Option[String]],
      any[Option[String]],
      any[Option[Seq[Attachment]]],
      any[Option[Boolean]],
      any[Option[Boolean]],
      any[Option[String]],
      any[Option[String]],
      any[Option[Boolean]],
      any[Option[Boolean]],
      any[Option[String]],
      any[Option[Boolean]])
    ).thenReturn(Future.successful(SlackMessage.blank))
    when(client.listConversations()).thenReturn(Future.successful(Seq()))
    client
  }

  def onEvent(event: Event): Future[Unit] = Future.successful({})

  def clientFor(botProfile: SlackBotProfile): SlackApiClient = client

  def slackUserDataList(slackUserIds: Set[String], botProfile: SlackBotProfile): Future[Set[SlackUserData]] = {
    Future.successful(Set())
  }

  def maybeSlackUserDataForAction(slackUserId: String, client: SlackApiClient, onUserNotFound: SlackApiError => Option[SlackUser]): DBIO[Option[SlackUserData]] = {
    DBIO.successful(None)
  }

  def maybeSlackUserDataFor(slackUserId: String, client: SlackApiClient, onUserNotFound: SlackApiError => Option[SlackUser]): Future[Option[SlackUserData]] = {
    Future.successful(None)
  }

  def maybeSlackUserDataForAction(botProfile: SlackBotProfile): DBIO[Option[SlackUserData]] = {
    DBIO.successful(Some(SlackUserData(botProfile.userId, None, SlackUserTeamIds(botProfile.slackTeamId), "MockBot", isPrimaryOwner = false, isOwner = false, isRestricted = false, isUltraRestricted = false, isBot = false, None, deleted = false, Some(SlackUserProfileData(Some("MockBot"), None, None, None, None, None)))))
  }

  def maybeSlackUserDataFor(botProfile: SlackBotProfile): Future[Option[SlackUserData]] = {
    Future.successful(Some(SlackUserData(botProfile.userId, None, SlackUserTeamIds(botProfile.slackTeamId), "MockBot", isPrimaryOwner = false, isOwner = false, isRestricted = false, isUltraRestricted = false, isBot = false, None, deleted = false, Some(SlackUserProfileData(Some("MockBot"), None, None, None, None, None)))))
  }

  def maybeSlackUserDataForEmail(email: String, client: SlackApiClient): Future[Option[SlackUserData]] = {
    Future.successful(None)
  }

  def isUserValidForBot(slackUserId: String, botProfile: SlackBotProfile, maybeEnterpriseId: Option[String]): Future[Boolean] = {
    Future.successful(true)
  }

}
