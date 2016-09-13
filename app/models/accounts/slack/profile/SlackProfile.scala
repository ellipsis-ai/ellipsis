package models.accounts.slack.profile

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.SocialProfile

case class SlackProfile(teamId: String, loginInfo: LoginInfo) extends SocialProfile
