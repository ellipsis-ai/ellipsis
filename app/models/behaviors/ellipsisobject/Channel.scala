package models.behaviors.ellipsisobject

import json.UserData
import models.accounts.slack.botprofile.SlackBotProfile
import services.DefaultServices
import slick.dbio.DBIO
import utils.{FutureSequencer, SlackChannels}

import scala.concurrent.ExecutionContext

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
      memberSlackUsers <- DBIO.from {
        FutureSequencer.sequence(memberIds, (slackUserId: String) => services.slackEventService.maybeSlackUserDataFor(slackUserId, client, _ => None)).map(_.flatten)
      }
      memberData <- UserData.allFromSlackUserDataListAction(memberSlackUsers.toSet, botProfile.teamId, services)
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
