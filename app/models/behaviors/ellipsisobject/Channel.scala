package models.behaviors.ellipsisobject

import json.{SlackUserData, UserData}
import models.accounts.slack.botprofile.SlackBotProfile
import services.DefaultServices
import slick.dbio.DBIO
import utils.SlackChannels

import scala.concurrent.{ExecutionContext, Future}

case class Channel(
                    id: String,
                    name: Option[String],
                    formattedLink: Option[String],
                    members: Option[Seq[UserData]]
                  )

object Channel {

  def buildForSlackAction(channelId: String, botProfile: SlackBotProfile, services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Channel] = {
    val client = services.slackApiService.clientFor(botProfile)
    val slackChannels = SlackChannels(client)
    for {
      maybeChannelInfo <- DBIO.from(slackChannels.getInfoFor(channelId))
      memberIds <- DBIO.from(slackChannels.getMembersFor(channelId))
      memberSlackUsers <- DBIO.from(Future.sequence(memberIds.map(ea => client.getUserInfo(ea)))).map(_.flatten)
      memberSlackUserData <- DBIO.successful(memberSlackUsers.map(u => SlackUserData.fromSlackUser(u, botProfile)))
      memberData <- UserData.allFromSlackUserDataListAction(memberSlackUserData.toSet, botProfile.teamId, services)
    } yield{
      Channel(
        channelId,
        maybeChannelInfo.flatMap(_.name),
        Some(s"<#${channelId}>"),
        Some(memberData.toSeq)
      )
    }
  }

}
