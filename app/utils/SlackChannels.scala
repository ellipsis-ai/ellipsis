package utils

import akka.actor.ActorSystem
import services.caching.{CacheService, SlackChannelDataCacheKey, SlackGroupDataCacheKey}
import slack.api.{ApiError, SlackApiClient}
import slack.models.{Channel, Group, Im}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

sealed trait SlackChannelException {
  val underlying: Throwable
  val slackTeamId: String
}

case class ChannelInfoException(
                                 channel: String,
                                 slackTeamId: String,
                                 underlying: Throwable
                               ) extends Exception(
  s"Error fetching info for Slack channel $channel on team $slackTeamId because $underlying",
  underlying) with SlackChannelException

case class GroupInfoException(
                               group: String,
                               slackTeamId: String,
                               underlying: Throwable
                             ) extends Exception(
  s"Error fetching info for Slack group $group on team $slackTeamId because $underlying",
  underlying) with SlackChannelException

case class ListChannelsException(
                                  slackTeamId: String,
                                  underlying: Throwable
                                ) extends Exception(
  s"Error fetching list of Slack channels on team $slackTeamId because $underlying",
  underlying) with SlackChannelException

case class ListGroupsException(
                                slackTeamId: String,
                                underlying: Throwable
                              ) extends Exception(
  s"Error fetching list of Slack groups on team $slackTeamId because $underlying",
  underlying) with SlackChannelException

case class DMInfoException(
                            slackTeamId: String,
                            underlying: Throwable
                          ) extends Exception(
  s"Error fetching list of Slack DM channels on team $slackTeamId because $underlying",
  underlying) with SlackChannelException

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

case class SlackChannels(client: SlackApiClient, cacheService: CacheService, slackTeamId: String) {

  def getInfoFor(channelLikeId: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[ChannelLike]] = {
    for {
      maybeChannel <- maybeChannelInfoFor(channelLikeId)
      maybeGroup <- if (maybeChannel.isEmpty) {
        maybeGroupInfoFor(channelLikeId)
      } else {
        Future.successful(None)
      }
      maybeIm <- if (maybeChannel.isEmpty && maybeGroup.isEmpty) {
        listIms.map(_.find(_.id == channelLikeId))
      } else {
        Future.successful(None)
      }
    } yield {
      maybeChannel.map(SlackChannel.apply) orElse {
        maybeGroup.map(SlackGroup.apply) orElse {
          maybeIm.map(SlackDM.apply)
        }
      }
    }
  }

  def getList(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[ChannelLike]] = {
    for {
      channels <- listChannels
      groups <- listGroups
      dms <- listIms
    } yield {
      channels.map(SlackChannel.apply) ++ groups.map(SlackGroup.apply) ++ dms.map(SlackDM.apply)
    }
  }

  def getListForUser(maybeSlackUserId: Option[String])(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[ChannelLike]] = {
    maybeSlackUserId.map { slackUserId =>
      getList.map { channels =>
        channels.filter(ea => ea.visibleToUser(slackUserId))
      }
    }.getOrElse(Future.successful(Seq()))
  }

  def getMembersFor(channelOrGroupId: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[String]] = {
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

  def maybeIdFor(channelLikeIdOrName: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    val unformattedChannelLikeIdOrName = unformatChannelText(channelLikeIdOrName)
    getInfoFor(unformattedChannelLikeIdOrName).flatMap { maybeChannelLike =>
      maybeChannelLike.map(c => Future.successful(Some(c.id))).getOrElse {
        getList.map { infos =>
          infos.find(_.name == unformattedChannelLikeIdOrName).map(_.id)
        }
      }
    }
  }

  def maybeChannelInfoFor(channel: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[Channel]] = {
    if (!channel.startsWith("C")) {
      Future.successful(None)
    } else {
      cacheService.getSlackChannelInfo(SlackChannelDataCacheKey(channel, slackTeamId), (key: SlackChannelDataCacheKey) => {
        client.getChannelInfo(key.channel).map(Some(_)).recover {
          case e: ApiError => if (e.code == "channel_not_found") {
            None
          } else {
            throw ChannelInfoException(channel, slackTeamId, e)
          }
        }
      })
    }
  }

  def maybeGroupInfoFor(channel: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[Group]] = {
    if (!channel.startsWith("G")) {
      Future.successful(None)
    } else {
      cacheService.getSlackGroupInfo(SlackGroupDataCacheKey(channel, slackTeamId), (key: SlackGroupDataCacheKey) => {
        client.getGroupInfo(key.group).map(Some(_)).recover {
          case e: ApiError => if (e.code == "channel_not_found") {
            None
          } else {
            throw GroupInfoException(channel, slackTeamId, e)
          }
        }
      })
    }
  }

  def listChannels(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[Channel]] = {
    cacheService.getSlackChannels(slackTeamId, (slackTeamId: String) => {
      client.listChannels(excludeArchived = 1).recover {
        case t: Throwable => throw ListChannelsException(slackTeamId, t)
      }
    })
  }

  def listGroups(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[Group]] = {
    cacheService.getSlackGroups(slackTeamId, (slackTeamId: String) =>
      client.listGroups(excludeArchived = 1).recover {
        case t: Throwable => throw ListGroupsException(slackTeamId, t)
      }
    )
  }

  def listIms(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Seq[Im]] = {
    cacheService.getSlackIMs(slackTeamId, (slackTeamId: String) =>
      client.listIms().recover {
        case t: Throwable => throw DMInfoException(slackTeamId, t)
      }
    )
  }
}

object SlackChannelsRegexes {

  val unformatSlackChannelRegex: Regex = """<#(.+)\|.+>""".r
  val unformatHashPrefixRegex: Regex = """#(.+)""".r

}
