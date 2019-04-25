package models.accounts.slack.slackmemberstatus

import scala.concurrent.Future

trait SlackMemberStatusService {

  def updateAll: Future[Unit]
}
