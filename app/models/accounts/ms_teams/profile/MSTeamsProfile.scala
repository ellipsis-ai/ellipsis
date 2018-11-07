package models.accounts.ms_teams.profile

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.SocialProfile
import models.accounts.slack.SlackUserTeamIds

case class MSTeamsProfile(teamId: String, loginInfo: LoginInfo, maybeEnterpriseId: Option[String]) extends SocialProfile {
  def msTeamsUserId = loginInfo.providerKey
}
