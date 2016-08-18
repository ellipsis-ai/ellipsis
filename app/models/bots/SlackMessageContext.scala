package models.bots

import models.Team
import models.accounts.{OAuth2Token, SlackBotProfile}
import models.bots.conversations.{ConversationQueries, Conversation}
import models.bots.templates.SlackRenderer
import slack.api.SlackApiClient
import slack.models.Message
import slack.rtm.SlackRtmClient
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.matching.Regex

case class SlackMessageContext(
                        client: SlackRtmClient,
                        profile: SlackBotProfile,
                        message: Message
                        ) extends MessageContext {

  val fullMessageText = message.text

  val teamId: String = profile.teamId

  lazy val botId: String = client.state.self.id
  lazy val name: String = Conversation.SLACK_CONTEXT
  lazy val userIdForContext: String = message.user

  lazy val isDirectMessage: Boolean = {
    message.channel.startsWith("D")
  }

  lazy val relevantMessageText: String = {
    var text = message.text
    text = SlackMessageContext.toBotRegexFor(botId).replaceFirstIn(text, "")
    text = MessageContext.ellipsisRegex.replaceFirstIn(text, "")
    text
  }

  lazy val includesBotMention: Boolean = {
    isDirectMessage ||
      SlackMessageContext.mentionRegexFor(botId).findFirstMatchIn(message.text).nonEmpty ||
      MessageContext.ellipsisRegex.findFirstMatchIn(message.text).nonEmpty
  }

  lazy val isResponseExpected: Boolean = includesBotMention

  def sendMessage(unformattedText: String)(implicit ec: ExecutionContext): Unit = {
    val formattedText = SlackMessageFormatter(client).bodyTextFor(unformattedText)
    // The Slack API considers sending an empty message to be an error rather than a no-op
    if (formattedText.nonEmpty) {
      client.apiClient.postChatMessage(message.channel, formattedText, asUser = Some(true))
    }
  }

  override def recentMessages: DBIO[Seq[String]] = {
    for {
      maybeTeam <- Team.find(profile.teamId)
      maybeOAuthToken <- OAuth2Token.maybeFullForSlackTeamId(profile.slackTeamId)
      maybeUserClient <- DBIO.successful(maybeOAuthToken.map { token =>
        SlackApiClient(token.accessToken)
      })
      maybeHistory <- maybeUserClient.map { userClient =>
        DBIO.from(userClient.getChannelHistory(message.channel, latest = Some(message.ts))).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      messages <- DBIO.successful(maybeHistory.map { history =>
        history.messages.slice(0, 10).reverse.flatMap { json =>
          (json \ "text").asOpt[String]
        }
      }.getOrElse(Seq()))
    } yield messages
  }

  def maybeOngoingConversation: DBIO[Option[Conversation]] = {
    ConversationQueries.findOngoingFor(message.user, Conversation.SLACK_CONTEXT)
  }
}

object SlackMessageContext {

  def mentionRegexFor(botId: String): Regex = s"""<@$botId>""".r
  def toBotRegexFor(botId: String): Regex = s"""^<@$botId>:?\\s*""".r
}
