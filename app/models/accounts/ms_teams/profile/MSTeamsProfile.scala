package models.accounts.ms_teams.profile

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.SocialProfile

case class MSTeamsProfile(teamId: String, loginInfo: LoginInfo) extends SocialProfile {
  def msTeamsUserId = loginInfo.providerKey
}
