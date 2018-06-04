package utils

import models.accounts.slack.botprofile.SlackBotProfile
import services.SlackApiService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

case class SlackChannels(profile: SlackBotProfile, apiService: SlackApiService) {

  def getInfoFor(convoId: String)(implicit ec: ExecutionContext): Future[Option[SlackConversation]] = {
    apiService.conversationInfo(profile, convoId)
  }

  def getList(implicit ec: ExecutionContext): Future[Seq[SlackConversation]] = {
    apiService.listConversations(profile)
  }

  def getMembersFor(convoId: String)(implicit ec: ExecutionContext): Future[Seq[String]] = {
    apiService.conversationMembers(profile, convoId)
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
          infos.find(_.name == unformattedChannelLikeIdOrName).map(_.id)
        }
      }
    }
  }

}

object SlackChannelsRegexes {

  val unformatSlackChannelRegex: Regex = """<#(.+)\|.+>""".r
  val unformatHashPrefixRegex: Regex = """#(.+)""".r

}
