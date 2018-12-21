package models.behaviors.builtins.admin

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json.SlackUserData
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.slack.SlackProvider
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behaviorversion.Normal
import models.behaviors.builtins.BuiltinBehavior
import models.behaviors.events.Event
import models.behaviors.{BotResult, NoResponseForBuiltinResult, SimpleTextResult}
import models.team.Team
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class LookupSlackUserInfo(slackUserData: SlackUserData, teams: Seq[Team])

case class LookupSlackUserBehavior(slackUserId: String, maybeEllipsisTeamId: Option[String], maybeSlackTeamId: Option[String], event: Event, services: DefaultServices) extends BuiltinBehavior {
  private val dataService = services.dataService

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      user <- event.ensureUser(dataService)
      isAdmin <- dataService.users.isAdmin(user)
      result <- if (isAdmin) {
        lookupResult
      } else {
        Future.successful(NoResponseForBuiltinResult(event))
      }
    } yield result
  }

  private def lookupResult(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    for {
      linkedAccounts <- dataService.linkedAccounts.allForLoginInfo(LoginInfo(SlackProvider.ID, slackUserId))
      maybeLookupInfo <- maybeEllipsisTeamId.map { ellipsisTeamId =>
        maybeForEllipsisTeam(ellipsisTeamId)
      }.orElse(maybeSlackTeamId.map { slackTeamId =>
        maybeForSlackTeam(slackTeamId)
      }).getOrElse {
        maybeFromLinkedAccounts(linkedAccounts)
      }
    } yield {
      val message = maybeLookupInfo.map { lookupInfo =>
        val userData = lookupInfo.slackUserData
        val slackUserInfo =
          s"""Found Slack user with ID `${slackUserId}` named @${userData.getDisplayName}:
             |- full name: ${userData.maybeRealName.getOrElse("(not provided)")}
             |- email: ${userData.profile.flatMap(_.email).getOrElse("(not provided)")}
             |- phone: ${userData.profile.flatMap(_.phone).getOrElse("(not provided)")}
             |- tz: ${userData.tz.getOrElse("(not provided)")}
             |- Slack team IDs: ${userData.accountTeamIds.mkString("`", "`, `", "`")}
         """.stripMargin

        val teamInfo = if (lookupInfo.teams.nonEmpty) {
          val teamList = lookupInfo.teams.map { team =>
            val teamName = team.maybeNonEmptyName.getOrElse("(no name)")
            val maybeLinkedAccountInfo = linkedAccounts.find(_.user.teamId == team.id).map { linkedAccount =>
              s" with linked account as Ellipsis user ID `${linkedAccount.user.id}`\n"
            }.getOrElse("")
            s"- _${teamName}_ (ID `${team.id}`)${maybeLinkedAccountInfo}"
          }.mkString("")
          val otherLinkedAccounts = linkedAccounts.filter(linkedAccount => !lookupInfo.teams.exists(_.id == linkedAccount.user.teamId))
          val otherLinkedAccountInfo = if (linkedAccounts.nonEmpty) {
            if (otherLinkedAccounts.nonEmpty) {
              "Other linked accounts:\n" ++ otherLinkedAccounts.map { linkedAccount =>
                s"- Ellipsis user ID ${linkedAccount.user.id} on team ID ${linkedAccount.user.teamId}\n"
              }.mkString("")
            } else {
              ""
            }
          } else {
            "No linked accounts for this user were found."
          }
          s"""Related Ellipsis teams:
             |${teamList}
             |${otherLinkedAccountInfo}
           """.stripMargin
        } else {
          "No relevant Ellipsis teams or linked accounts found."
        }

        s"""${slackUserInfo}
           |
           |${teamInfo}
         """.stripMargin
      }.getOrElse {
        s"No Slack user data found for Slack user ID `${slackUserId}`"
      }

      SimpleTextResult(event, None, message, Normal)
    }
  }

  private def maybeForEllipsisTeam(ellipsisTeamId: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[LookupSlackUserInfo]] = {
    for {
      maybeTeam <- dataService.teams.find(ellipsisTeamId)
      slackBotProfiles <- maybeTeam.map { team =>
        dataService.slackBotProfiles.allFor(team)
      }.getOrElse(Future.successful(Seq()))
      maybeUserData <- maybeUserDataFromSlackBotProfiles(slackBotProfiles)
    } yield {
      maybeUserData.map { userData =>
        val teams = maybeTeam.map(Seq(_)).getOrElse(Seq())
        LookupSlackUserInfo(userData, teams)
      }
    }
  }

  private def maybeForSlackTeam(slackTeamId: String)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[LookupSlackUserInfo]] = {
    for {
      slackBotProfiles <- dataService.slackBotProfiles.allForSlackTeamId(slackTeamId)
      teamsForBots <- Future.sequence(slackBotProfiles.map(_.teamId).distinct.map { teamId =>
        dataService.teams.find(teamId)
      }).map(_.flatten)
      maybeUserData <- maybeUserDataFromSlackBotProfiles(slackBotProfiles)
    } yield maybeUserData.map { userData =>
      val botsForUser = slackBotProfiles.filter(profile => userData.accountTeamIds.contains(profile.slackTeamId))
      val teamsForUser = teamsForBots.filter(team => botsForUser.exists(_.teamId == team.id))
      LookupSlackUserInfo(userData, teamsForUser)
    }
  }

  private def maybeFromLinkedAccounts(linkedAccounts: Seq[LinkedAccount])(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[LookupSlackUserInfo]] = {
    for {
      teams <- Future.sequence(linkedAccounts.map(ea => dataService.teams.find(ea.user.teamId))).map(_.flatten)
      maybeUserData <- firstSlackUserFor(linkedAccounts, (linkedAccount: LinkedAccount) => dataService.users.maybeSlackUserDataFor(linkedAccount.user))
    } yield {
      maybeUserData.map { userData =>
        LookupSlackUserInfo(userData, teams)
      }
    }
  }

  private def maybeUserDataFromSlackBotProfiles(profiles: Seq[SlackBotProfile])(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[SlackUserData]] = {
    firstSlackUserFor(profiles, (slackBotProfile: SlackBotProfile) => {
      val client = services.slackApiService.clientFor(slackBotProfile)
      services.slackEventService.maybeSlackUserDataFor(slackUserId, client, (_) => None)
    })
  }

  private def firstSlackUserFor[T](list: Seq[T], findFunction: T => Future[Option[SlackUserData]])
                                  (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[SlackUserData]] = {
    list.foldLeft(Future.successful(Option.empty[SlackUserData])) { (futureMaybeData, item) =>
      futureMaybeData.flatMap {
        case None => findFunction(item)
        case result => Future.successful(result)
      }
    }
  }
}
