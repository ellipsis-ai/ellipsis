package models.behaviors.ellipsisobject

import models.accounts.slack.botprofile.SlackBotProfile
import services.DefaultServices
import slick.dbio.DBIO
import utils.SlackChannels

import scala.concurrent.ExecutionContext

case class Channel(
                    id: String,
                    name: Option[String],
                    formattedLink: Option[String]
                  )

object Channel {

  def buildForSlackAction(channelId: String, botProfile: SlackBotProfile, services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Channel] = {
    val client = services.slackApiService.clientFor(botProfile)
    val slackChannels = SlackChannels(client)
    for {
      maybeChannelInfo <- DBIO.from(slackChannels.getInfoFor(channelId))
    } yield{
      Channel(
        channelId,
        maybeChannelInfo.flatMap(_.name),
        Some(s"<#${channelId}>")
      )
    }
  }

}
