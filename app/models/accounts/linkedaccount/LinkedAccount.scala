package models.accounts.linkedaccount

import java.time.OffsetDateTime

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User

case class LinkedAccount(user: User, loginInfo: LoginInfo, createdAt: OffsetDateTime) {
  def toRaw: RawLinkedAccount = RawLinkedAccount(user.id, loginInfo, createdAt)
}

object LinkedAccount {
  val ELLIPSIS_SLACK_TEAM_ID = "T0LP53H0A"
  val ELLIPSIS_SLACK_FEEDBACK_CHANNEL_ID = "C79QYK4A0"
  val ELLIPSIS_MANAGED_SKILL_ERRORS_CHANNEL_ID = "CBJFPHF53"
  val ELLIPSIS_MONITORING_CHANNEL_ID = "CCDQMGD7H"
}
