package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global

trait SlackEvent {
  val user: String
  val profile: SlackBotProfile
  def clientFor(dataService: DataService) = dataService.slackBotProfiles.clientFor(profile)
  def eventualMaybeDMChannel(dataService: DataService)(implicit actorSystem: ActorSystem) = {
    dataService.slackBotProfiles.clientFor(profile).listIms.map(_.find(_.user == user).map(_.id))
  }

  def isDirectMessage(channelId: String): Boolean = {
    channelId.startsWith("D")
  }
  def isPrivateChannel(channelId: String): Boolean = {
    channelId.startsWith("G")
  }

}
