package mocks

import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.events.EventHandler
import org.scalatest.mock.MockitoSugar
import services.SlackService
import slack.rtm.SlackRtmClient

class MockSlackService extends SlackService with MockitoSugar {

  val clients = mock[scala.collection.mutable.Map[SlackBotProfile, SlackRtmClient]]

  val eventHandler = mock[EventHandler]

  def startFor(profile: SlackBotProfile): Unit = Unit
  def stopFor(profile: SlackBotProfile): Unit = Unit

}

