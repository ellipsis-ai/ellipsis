package models.accounts.slack.slackmemberstatus

import services.slack.apiModels.MembershipData

import scala.concurrent.Future

trait SlackMemberStatusService {

  def updateFor(membershipData: MembershipData): Future[Unit]
}
