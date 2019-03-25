package support

import models.accounts.slack.botprofile.SlackBotProfile
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import services.slack.{SlackApiClient, SlackApiService}
import utils.SlackConversation

import scala.concurrent.Future

trait SlackContext extends MockitoSugar {

  def newMockSlackApiClientFor(
                                slackApiService: SlackApiService,
                                profile: SlackBotProfile,
                                channel: String,
                                members: Seq[String] = Seq(),
                                maybeChannelName: Option[String] = None
                              ): SlackApiClient = {
    val channelName = maybeChannelName.getOrElse(channel)
    val client = mock[SlackApiClient]
    when(slackApiService.clientFor(profile)).thenReturn(client)
    when(client.conversationInfo(channel)).thenReturn(Future.successful(Some(SlackConversation.defaultFor(channel, channelName))))
    when(client.conversationMembers(channel)).thenReturn(Future.successful(members))
    when(client.openConversationFor(anyString)).thenReturn(Future.successful("D123456"))
    when(client.permalinkFor(anyString, anyString)).thenReturn(Future.successful(None))
    when(client.profile).thenReturn(profile)
    client
  }
}
