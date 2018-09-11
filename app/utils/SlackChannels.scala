package utils

import services.slack.SlackApiClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

case class SlackChannels(client: SlackApiClient) {

  def getInfoFor(convoId: String)(implicit ec: ExecutionContext): Future[Option[SlackConversation]] = {
    client.conversationInfo(convoId)
  }

  def getList(implicit ec: ExecutionContext): Future[Seq[SlackConversation]] = {
    client.listConversations()
  }

  def getMembersFor(convoId: String)(implicit ec: ExecutionContext): Future[Seq[String]] = {
    client.conversationMembers(convoId)
  }

  import SlackChannelsRegexes._

  def unformatChannelText(channelText: String): String = {
    channelText match {
      case unformatSlackChannelRegex(channelId) => channelId
      case unformatHashPrefixRegex(channelName) => channelName
      case _ => channelText
    }
  }

  def maybeIdFor(channelLikeIdOrName: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    val unformattedChannelLikeIdOrName = unformatChannelText(channelLikeIdOrName)
    getInfoFor(unformattedChannelLikeIdOrName).flatMap { maybeChannelLike =>
      maybeChannelLike.map(c => Future.successful(Some(c.id))).getOrElse {
        getList.map { infos =>
          infos.find(_.name.contains(unformattedChannelLikeIdOrName)).map(_.id)
        }
      }
    }
  }

}

object SlackChannelsRegexes {

  val unformatSlackChannelRegex: Regex = """<#(.+)\|.+>""".r
  val unformatHashPrefixRegex: Regex = """#(.+)""".r

}
