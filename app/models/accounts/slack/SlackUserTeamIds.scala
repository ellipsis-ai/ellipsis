package models.accounts.slack

import utils.NonEmptyStringSet

case class SlackUserTeamIds(override val head: String, others: Seq[String] = Seq.empty) extends NonEmptyStringSet {
  def primary: String = head
}
