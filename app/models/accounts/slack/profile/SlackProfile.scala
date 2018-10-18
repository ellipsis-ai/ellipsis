package models.accounts.slack.profile

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.SocialProfile
import utils.NonEmptyStringSet

case class SlackProfile(teamIds: NonEmptyStringSet, loginInfo: LoginInfo, maybeEnterpriseId: Option[String]) extends SocialProfile {
  val firstTeamId: String = teamIds.head
}
