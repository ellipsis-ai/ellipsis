package utils

import akka.actor.ActorSystem
import slack.api.{ApiError, SlackApiClient}
import slack.models.{Channel, Group, Im}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

trait ChannelLike {
  val members: Seq[String]
  val id: String
  val name: String
  val isPublic: Boolean
  val isArchived: Boolean

  def visibleToUser(userId: String): Boolean = {
    isPublic || members.contains(userId)
  }
}

case class SlackChannel(channel: Channel) extends ChannelLike {
  val members: Seq[String] = channel.members.getOrElse(Seq())
  val id: String = channel.id
  val name: String = channel.name
  val isPublic: Boolean = true
  val isArchived: Boolean = channel.is_archived.getOrElse(false)
}

case class SlackGroup(group: Group) extends ChannelLike {
  // Slack API hard-codes topic value "Group messaging" for group DMs
  val isGroupDM: Boolean = group.topic.value == "Group messaging"
  val members: Seq[String] = group.members
  val id: String = group.id
  // Slack API returns list of user names in group DMs inside the group "purpose"
  val name: String = if (isGroupDM) {
    group.purpose.value
  } else {
    group.name
  }
  val isPublic: Boolean = false
  val isArchived: Boolean = group.is_archived
}

case class SlackDM(im: Im) extends ChannelLike {
  val members: Seq[String] = Seq(im.user)
  val id: String = im.id
  val name: String = id
  val isPublic: Boolean = false
  val isArchived: Boolean = im.is_user_deleted.getOrElse(false)
}

case class SlackChannels(client: SlackApiClient) {

  private def swallowingChannelNotFound[T](fn: () => Future[T]): Future[Option[T]] = {
    fn().map(Some(_)).recover {
      case e: ApiError => if (e.code == "channel_not_found") {
        None
      } else {
        throw e
      }
    }
  }

  private def getInfoFor(channelLikeId: String)(implicit actorSystem: ActorSystem): Future[Option[ChannelLike]] = {
    for {
      maybeChannel <- swallowingChannelNotFound(() => client.getChannelInfo(channelLikeId))
      maybeGroup <- swallowingChannelNotFound(() => client.getGroupInfo(channelLikeId))
      maybeIm <- client.listIms().map(_.find(_.id == channelLikeId))
    } yield {
      maybeChannel.map(SlackChannel.apply).orElse(maybeGroup.map(SlackGroup.apply)).orElse(maybeIm.map(SlackDM.apply))
    }
  }

  def getList(implicit actorSystem: ActorSystem): Future[Seq[ChannelLike]] = {
    for {
      channels <- client.listChannels()
      groups <- client.listGroups()
      dms <- client.listIms()
    } yield {
      channels.map(SlackChannel.apply) ++ groups.map(SlackGroup.apply) ++ dms.map(SlackDM.apply)
    }
  }

  def getListForUser(maybeSlackUserId: Option[String])(implicit actorSystem: ActorSystem): Future[Seq[ChannelLike]] = {
    maybeSlackUserId.map { slackUserId =>
      getList.map { channels =>
        channels.filter(ea => ea.visibleToUser(slackUserId))
      }
    }.getOrElse(Future.successful(Seq()))
  }

  def getMembersFor(channelOrGroupId: String)(implicit actorSystem: ActorSystem): Future[Seq[String]] = {
    getInfoFor(channelOrGroupId).map { maybeChannelLike =>
      maybeChannelLike.map(_.members).getOrElse(Seq())
    }
  }

  import SlackChannelsRegexes._

  def unformatChannelText(channelText: String): String = {
    channelText match {
      case unformatSlackChannelRegex(channelId) => channelId
      case unformatHashPrefixRegex(channelName) => channelName
      case _ => channelText
    }
  }

  def maybeIdFor(channelLikeIdOrName: String)(implicit actorSystem: ActorSystem): Future[Option[String]] = {
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
