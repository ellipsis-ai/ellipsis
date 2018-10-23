package models.accounts.slack.profile

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.SocialProfile
import models.accounts.slack.SlackUserTeamIds

case class SlackProfile(teamIds: SlackUserTeamIds, loginInfo: LoginInfo, maybeEnterpriseId: Option[String]) extends SocialProfile {
  val firstTeamId: String = teamIds.head
  def slackUserId = loginInfo.providerKey
}
