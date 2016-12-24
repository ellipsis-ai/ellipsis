package services.slack

import models.accounts.slack.botprofile.SlackBotProfile
import slack.api.SlackApiClient

trait SlackEvent {
  val profile: SlackBotProfile
  lazy val client = new SlackApiClient(profile.token)
}
