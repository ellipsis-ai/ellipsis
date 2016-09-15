package services

import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.EventHandler
import slack.rtm.SlackRtmClient

trait SlackService {

  val clients: scala.collection.mutable.Map[SlackBotProfile, SlackRtmClient]

  val eventHandler: EventHandler

  def startFor(profile: SlackBotProfile): Unit
  def stopFor(profile: SlackBotProfile): Unit

}
