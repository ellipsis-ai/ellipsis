package models.accounts.slack.slackmemberstatus

import scala.concurrent.Future

trait SlackMemberStatusService {

  val lastRunKey: String = "SlackMembershipStatusUpdateLastRun"

  def updateAll: Future[Unit]

  def allFor(slackTeamId: String): Future[Seq[SlackMemberStatus]]
}
