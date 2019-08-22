package json

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.user.User
import models.behaviors.conversations.conversation.Conversation
import services.DefaultServices
import services.ms_teams.apiModels.MSAADUser
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class UserData(
                      ellipsisUserId: String,
                      context: Option[String],
                      userName: Option[String],
                      userIdForContext: Option[String],
                      fullName: Option[String],
                      email: Option[String],
                      timeZone: Option[String],
                      formattedLink: Option[String]
                    ) {
  val userNameOrDefault: String = userName.getOrElse(s"User with ID <$ellipsisUserId>")
}

object UserData {

  def fromSlackUserData(user: User, slackUserData: SlackUserData): UserData = {
    UserData(
      ellipsisUserId = user.id,
      context = Some(Conversation.SLACK_CONTEXT),
      userName = Some(slackUserData.getDisplayName),
      userIdForContext = Some(slackUserData.accountId),
      fullName = slackUserData.profile.flatMap(_.realName),
      email = slackUserData.profile.flatMap(_.email),
      timeZone = slackUserData.tz,
      formattedLink = Some(s"<@${slackUserData.accountId}>")
    )
  }

  def fromMSAADUser(user: User, msTeamsUser: MSAADUser): UserData = {
    UserData(
      ellipsisUserId = user.id,
      context = Some(Conversation.MS_TEAMS_CONTEXT),
      userName = msTeamsUser.displayName,
      userIdForContext = Some(msTeamsUser.id),
      fullName = msTeamsUser.displayName,
      email = msTeamsUser.mail,
      timeZone = msTeamsUser.mailBoxSettings.flatMap(_.timeZone),
      formattedLink = msTeamsUser.formattedLink
    )
  }

  def allFromSlackUserDataListAction(
                                      userList: Set[SlackUserData],
                                      ellipsisTeamId: String,
                                      services: DefaultServices
                                    )(implicit ec: ExecutionContext): DBIO[Set[UserData]] = {
    DBIO.sequence(userList.toSeq.map { data =>
      services.dataService.users.ensureUserForAction(LoginInfo(Conversation.SLACK_CONTEXT, data.accountId), Seq(), ellipsisTeamId).map { user =>
        UserData.fromSlackUserData(user, data)
      }
    }).map(_.toSet)
  }

  def asAdmin(id: String): UserData = {
    UserData(
      ellipsisUserId = id,
      context = None,
      userName = Some("Ellipsis Admin"),
      userIdForContext = None,
      fullName = Some("Ellipsis Admin"),
      email = None,
      timeZone = None,
      formattedLink = Some("Ellipsis Admin")
    )
  }

  def withoutProfile(id: String): UserData = {
    UserData(
      ellipsisUserId = id,
      context = None,
      userName = None,
      userIdForContext = None,
      fullName = None,
      email = None,
      timeZone = None,
      formattedLink = None
    )
  }
}
