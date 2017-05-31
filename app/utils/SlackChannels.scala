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
}

case class SlackChannel(channel: Channel) extends ChannelLike {
  val members: Seq[String] = channel.members.getOrElse(Seq())
  val id: String = channel.id
  val name: String = channel.name
}

case class SlackGroup(group: Group) extends ChannelLike {
  val members: Seq[String] = group.members
  val id: String = group.id
  val name: String = group.name
}

case class SlackDM(im: Im) extends ChannelLike {
  val members: Seq[String] = Seq(im.user)
  val id: String = im.id
  val name: String = id
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

  def listInfos(implicit actorSystem: ActorSystem): Future[Seq[ChannelLike]] = {
    for {
      channels <- client.listChannels()
      groups <- client.listGroups()
      dms <- client.listIms()
    } yield {
      channels.map(SlackChannel.apply) ++ groups.map(SlackGroup.apply) ++ dms.map(SlackDM.apply)
    }
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
        listInfos.map { infos =>
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
