package services.ms_teams

import com.mohiva.play.silhouette.api.LoginInfo
import models.behaviors.conversations.conversation.Conversation
import services.DataService
import services.ms_teams.apiModels.{ChannelAccount, MSAADUser, MailBoxSettings, MentionEntity}

import scala.concurrent.{ExecutionContext, Future}

case class MSTeamsUser(
                      id: String,
                      aadId: Option[String],
                      displayName: Option[String],
                      givenName: Option[String],
                      surname: Option[String],
                      mail: Option[String],
                      mailBoxSettings: Option[MailBoxSettings]
                    ) {
  val formattedLink: Option[String] = displayName.map(d => s"<at>$d</at>")

  def maybeMentionEntity: Option[MentionEntity] = {
    displayName.map { name =>
      MentionEntity(ChannelAccount(id, name), s"<at>$name</at>")
    }
  }
}

object MSTeamsUser {

  def maybeForMSAADUser(msAADUser: MSAADUser, teamId: String, dataService: DataService)(implicit ec: ExecutionContext): Future[Option[MSTeamsUser]] = {
    for {
      user <- dataService.users.ensureUserFor(LoginInfo(Conversation.MS_AAD_CONTEXT, msAADUser.id), Seq(), teamId)
      maybeLinkedAccount <- dataService.linkedAccounts.maybeForMSTeamsFor(user)
    } yield {
      maybeLinkedAccount.map { linked =>
        MSTeamsUser(
          linked.loginInfo.providerKey,
          Some(msAADUser.id),
          msAADUser.displayName,
          msAADUser.givenName,
          msAADUser.surname,
          msAADUser.mail,
          msAADUser.mailBoxSettings
        )
      }
    }
  }

}
