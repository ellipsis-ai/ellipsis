package models.accounts.linkedaccount

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import org.joda.time.LocalDateTime

case class LinkedAccount(user: User, loginInfo: LoginInfo, createdAt: LocalDateTime) {
  def toRaw: RawLinkedAccount = RawLinkedAccount(user.id, loginInfo, createdAt)
}

object LinkedAccount {
  val ELLIPSIS_SLACK_TEAM_ID = "T0LP53H0A"
}
