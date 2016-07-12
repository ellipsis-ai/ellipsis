package models.bots

import models.bots.conversations.Conversation
import slack.rtm.SlackRtmClient
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext

case class SlackNoMessageContext(client: SlackRtmClient, channel: String) extends Context {

  def maybeOngoingConversation: DBIO[Option[Conversation]] = DBIO.successful(None)

  def sendMessage(unformattedText: String)(implicit ec: ExecutionContext): Unit = {
    val formattedText = SlackMessageFormatter(client).bodyTextFor(unformattedText)
    client.apiClient.postChatMessage(channel, formattedText, asUser = Some(true))
  }

}
