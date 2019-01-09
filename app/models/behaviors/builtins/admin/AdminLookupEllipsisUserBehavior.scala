package models.behaviors.builtins.admin

import akka.actor.ActorSystem
import models.behaviors.behaviorversion.Normal
import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.events.Event
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class AdminLookupEllipsisUserBehavior(ellipsisUserId: String, event: Event, services: DefaultServices) extends BuiltinAdminBehavior {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      maybeUser <- dataService.users.find(ellipsisUserId)
      maybeTeam <- maybeUser.map { user =>
        dataService.teams.find(user.teamId)
      }.getOrElse(Future.successful(None))
      maybeUserData <- (for {
        user <- maybeUser
        team <- maybeTeam
      } yield {
        dataService.users.userDataFor(user, team).map(Some(_))
      }).getOrElse(Future.successful(None))
      linkedAccounts <- maybeUser.map { user =>
        dataService.linkedAccounts.allFor(user)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      val result = (for {
        team <- maybeTeam
        userData <- maybeUserData
      } yield {
        val userInfo = s"""Found Ellipsis user ID `${ellipsisUserId}` on team **[${team.name}](${teamLinkFor(team.id)})** (ID `${team.id}`):
           |- platform: ${userData.context.getOrElse("(unknown)")}
           |- platform ID: `${userData.userIdForContext.getOrElse("(unknown)")}`
           |- username: ${userData.userName.getOrElse("(unknown)")}
           |- full name: ${userData.fullName.getOrElse("(unknown)")}
           |- email: ${userData.email.getOrElse("(unknown)")}
           |- tz: ${userData.timeZone.getOrElse("(unknown)")}
           |""".stripMargin

        val otherLinkedAccounts = linkedAccounts.filterNot { account =>
          userData.context.contains(account.loginInfo.providerID) && userData.userIdForContext.contains(account.loginInfo.providerKey)
        }
        val linkedAccountInfo = if (linkedAccounts.nonEmpty) {
          if (otherLinkedAccounts.nonEmpty) {
            s"Linked accounts:\n${otherLinkedAccounts.map { account =>
              s"- ${account.loginInfo.providerID}: `${account.loginInfo.providerKey}`"
            }.mkString("\n")}"
          } else {
            "No other linked accounts found."
          }
        } else {
          "⚠️ _Warning:_ no linked accounts found."
        }

        s"${userInfo}\n\n${linkedAccountInfo}"
      }).getOrElse {
        s"""No user found with ID `${ellipsisUserId}`."""
      }
      SimpleTextResult(event, None, result, Normal)
    }
  }
}
