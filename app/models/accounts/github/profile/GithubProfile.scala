package models.accounts.github.profile

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.SocialProfile

case class GithubProfile(loginInfo: LoginInfo, token: String) extends SocialProfile
