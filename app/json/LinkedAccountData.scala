package json

import java.time.OffsetDateTime

import models.accounts.linkedaccount.LinkedAccount

case class LinkedAccountData(providerId: String, providerKey: String, createdAt: OffsetDateTime)

object LinkedAccountData {
  def from(linked: LinkedAccount): LinkedAccountData = {
    LinkedAccountData(linked.loginInfo.providerID, linked.loginInfo.providerKey, linked.createdAt)
  }
}
