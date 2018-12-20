package models.behaviors.builtins.admin

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json.SlackUserData
import models.accounts.slack.SlackProvider
import models.behaviors.behaviorversion.Normal
import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.builtins.BuiltinBehavior
import models.behaviors.events.Event
import play.api.Logger
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class LookupSlackUserBehavior(slackUserId: String, event: Event, services: DefaultServices) extends BuiltinBehavior {
  private val dataService = services.dataService

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      user <- event.ensureUser(dataService)
      isAdmin <- dataService.users.isAdmin(user)
      result <- if (isAdmin) {
        lookupResultFor(slackUserId, event, services)
      } else {
        Logger.warn(s"Ellipsis user ${user.id} on Ellipsis team ${user.teamId} tried to run an admin lookup")
        event.noExactMatchResult(services)
      }
    } yield result
  }

  private def lookupResultFor(slackUserId: String, event: Event, services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      linkedAccounts <- dataService.linkedAccounts.allForLoginInfo(LoginInfo(SlackProvider.ID, slackUserId))
      teams <- Future.sequence(linkedAccounts.map(ea => dataService.teams.find(ea.user.teamId))).map(_.flatten)
      maybeUserData <- linkedAccounts.foldLeft(Future.successful(Option.empty[SlackUserData])) { (futureMaybeData, linkedAccount) =>
        futureMaybeData.flatMap {
          case None => dataService.users.maybeSlackUserDataFor(linkedAccount.user)
          case result => Future.successful(result)
        }
      }
    } yield {
      val linkedAccountInfo = if (linkedAccounts.nonEmpty) {
        linkedAccounts.map { linkedAccount =>
          val user = linkedAccount.user
          val maybeTeam = teams.find(_.id == user.teamId)
          val teamInfo = maybeTeam.map { team =>
            s"_${team.maybeNonEmptyName.map(_.trim).getOrElse("(empty name)")}_ (ID `${team.id}`)"
          }.getOrElse {
            Logger.error(
              s"""Linked Slack account discovered for a user whose team does not exist:
                 |- user ID ${user.id}
                 |- team ID ${user.teamId}
                 |- Slack user ID ${linkedAccount.loginInfo.providerKey}
               """.stripMargin)
            s"team ID `${user.teamId}` **(Team not found!)**"
          }
          s"""Linked account:
             |- Ellipsis user ID `${linkedAccount.user.id}`
             |- Team: ${teamInfo}
             |""".stripMargin
        }.mkString("\n\n")
      } else {
        s"No linked account found for Slack user ID `${slackUserId}`"
      }

      val slackUserInfo = maybeUserData.map { userData =>
        s"""Found Slack user with ID `${slackUserId}` named @${userData.getDisplayName}:
           |- full name: ${userData.maybeRealName.getOrElse("(not provided)")}
           |- email: ${userData.profile.flatMap(_.email).getOrElse("(not provided)")}
           |- phone: ${userData.profile.flatMap(_.phone).getOrElse("(not provided)")}
           |- tz: ${userData.tz.getOrElse("(not provided)")}
           |- team IDs: ${userData.accountTeamIds.mkString("`", "`, `", "`")}
         """.stripMargin
      }.getOrElse {
        if (linkedAccounts.nonEmpty) {
          s"No Slack user data found for Slack user ID `${slackUserId}`"
        } else {
          ""
        }
      }

      val message =
        s"""${slackUserInfo}
           |
           |${linkedAccountInfo}
         """.stripMargin
      SimpleTextResult(event, None, message, Normal)
    }
  }
}
