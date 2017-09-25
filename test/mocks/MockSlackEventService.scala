package mocks

import akka.actor.ActorSystem
import json.SlackUserData
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.SlackMessageEvent
import org.mockito.Mockito.when
import services.SlackEventService
import slack.api.SlackApiClient
import org.scalatest.mock.MockitoSugar

import scala.concurrent.Future

class MockSlackEventService extends SlackEventService with MockitoSugar {
  def onEvent(event: SlackMessageEvent): Future[Unit] = Future.successful({})

  def clientFor(botProfile: SlackBotProfile): SlackApiClient = {
    val client = mock[SlackApiClient]
    implicit val system = ActorSystem("slack")
    when(client.listIms()(system)).thenReturn(Future.successful(Seq()))
    client
  }

  def slackUserDataList(slackUserIds: Set[String], botProfile: SlackBotProfile): Future[Set[SlackUserData]] = {
    Future.successful(Set())
  }

  def maybeSlackUserDataFor(slackUserId: String, slackTeamId: String, client: SlackApiClient): Future[Option[SlackUserData]] = {
    Future.successful(None)
  }

  def maybeSlackUserDataFor(botProfile: SlackBotProfile): Future[Option[SlackUserData]] = {
    Future.successful(None)
  }

}
