package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import slack.api.SlackApiClient

import scala.concurrent.ExecutionContext.Implicits.global

trait SlackEvent {
  val user: String
  val profile: SlackBotProfile
  lazy val client = new SlackApiClient(profile.token)
  def eventualMaybeDMChannel(implicit actorSystem: ActorSystem) = client.listIms.map(_.find(_.user == user).map(_.id))
}
