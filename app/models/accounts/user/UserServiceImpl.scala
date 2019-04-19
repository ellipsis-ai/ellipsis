package models.accounts.user

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.LoginInfo
import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import json.{SlackUserData, UserData}
import models.IDs
import models.accounts.linkedaccount.LinkedAccount
import models.accounts.ms_teams.profile.MSTeamsProfile
import models.accounts.slack.SlackUserTeamIds
import models.accounts.slack.profile.SlackProfile
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.events.slack.SlackMessageEvent
import models.team.Team
import play.api.Logger
import services.DefaultServices
import services.ms_teams.apiModels.MSAADUser
import services.slack.SlackApiClient

import scala.concurrent.{ExecutionContext, Future}

class UserServiceImpl @Inject() (
                                  servicesProvider: Provider[DefaultServices],
                                  implicit val actorSystem: ActorSystem,
                                  implicit val ec: ExecutionContext
                                ) extends UserService {

  def services: DefaultServices = servicesProvider.get

  def dataService = services.dataService
  def cacheService = services.cacheService
  def slackEventService = services.slackEventService
  def configuration = services.configuration
  def slackApiService = services.slackApiService
  def msTeamsApiService = services.msTeamsApiService
  def msTeamsEventService = services.msTeamsEventService

  import UserQueries._

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    if (loginInfo.providerID == User.EPHEMERAL_USER_ID) {
      val userId = loginInfo.providerKey
      find(userId)
    } else {
      Future.successful(None)
    }
  }

  def find(id: String): Future[Option[User]] = {
    dataService.run(findQueryFor(id).result.map(_.headOption))
  }

  def findFromEvent(event: Event, team: Team): Future[Option[User]] = {
    event match {
      case slackEvent: SlackMessageEvent => dataService.linkedAccounts.find(slackEvent.eventContext.loginInfo, team.id).map { maybeLinked =>
        maybeLinked.map(_.user)
      }
      case _ => Future.successful(None)
    }
  }

  def createOnTeamWithId(teamId: String): User = User(IDs.next, teamId, None)

  def createOnTeam(team: Team): User = createOnTeamWithId(team.id)

  def createFor(teamId: String): Future[User] = save(createOnTeamWithId(teamId))

  def saveAction(user: User): DBIO[User] = {
    val query = findQueryFor(user.id)
    query.result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === user.id).update(user)
      }.getOrElse {
        all += user
      }.map { _ => user }
    }
  }

  def save(user: User): Future[User] = {
    dataService.run(saveAction(user))
  }

  private def createNewUserAction(loginInfo: LoginInfo, teamId: String): DBIO[User] = {
    for {
      user <- saveAction(createOnTeamWithId(teamId))
      _ <- dataService.linkedAccounts.saveAction(LinkedAccount(user, loginInfo, OffsetDateTime.now))
    } yield user
  }

  private def maybeExistingUserForAction(loginInfo: LoginInfo, teamId: String): DBIO[Option[User]] = {
    dataService.linkedAccounts.findAction(loginInfo, teamId).map { maybeLinkedAccount =>
      maybeLinkedAccount.map(_.user)
    }
  }

  private def ensureUserForAction(loginInfo: LoginInfo, teamId: String): DBIO[User] = {
    maybeExistingUserForAction(loginInfo, teamId).flatMap { maybeExisting =>
      maybeExisting.map(DBIO.successful).getOrElse {
        createNewUserAction(loginInfo, teamId)
      }
    }
  }

  private def ensureUserFor(loginInfo: LoginInfo, teamId: String): Future[User] = {
    dataService.run(ensureUserForAction(loginInfo, teamId))
  }

  def ensureUserForAction(loginInfo: LoginInfo, otherLoginInfos: Seq[LoginInfo], teamId: String): DBIO[User] = {
    val loginInfos = Seq(loginInfo) ++ otherLoginInfos
    for {
      maybeExisting <- DBIO.sequence(loginInfos.map { loginInfo =>
        maybeExistingUserForAction(loginInfo, teamId)
      }).map(_.flatten.headOption)
      user <- maybeExisting.map(DBIO.successful).getOrElse {
        createNewUserAction(loginInfo, teamId)
      }
      _ <- DBIO.sequence(otherLoginInfos.map { ea =>
        dataService.linkedAccounts.saveAction(LinkedAccount(user, ea, OffsetDateTime.now))
      })
    } yield user
  }

  def ensureUserFor(loginInfo: LoginInfo, otherLoginInfos: Seq[LoginInfo], teamId: String): Future[User] = {
    dataService.run(ensureUserForAction(loginInfo, otherLoginInfos, teamId))
  }

  def teamAccessForAction(user: User, maybeTargetTeamId: Option[String]): DBIO[UserTeamAccess] = {
    for {
      loggedInTeam <- dataService.teams.findAction(user.teamId).map(_.get)
      isAdminUser <- DBIO.from(isAdmin(user))
      maybeTeam <- maybeTargetTeamId.map { targetTeamId =>
        if (targetTeamId != user.teamId && !isAdminUser) {
          DBIO.successful(None)
        } else {
          dataService.teams.findAction(targetTeamId)
        }
      }.getOrElse {
        dataService.teams.findAction(user.teamId)
      }
      maybeBotProfile <- maybeTeam.map { team =>
        dataService.slackBotProfiles.maybeFirstForAction(team, user)
      }.getOrElse(DBIO.successful(None))
      maybeBotName <- maybeBotProfile.map { botProfile =>
        DBIO.from(dataService.slackBotProfiles.maybeNameFor(botProfile))
      }.getOrElse(DBIO.successful(None))
    } yield UserTeamAccess(user, loggedInTeam, maybeTeam, maybeBotName, maybeTeam.exists(t => t.id != user.teamId), isAdminUser)
  }

  def teamAccessFor(user: User, maybeTargetTeamId: Option[String]): Future[UserTeamAccess] = {
    dataService.run(teamAccessForAction(user, maybeTargetTeamId))
  }

  def isAdmin(user: User): Future[Boolean] = {
    for {
      maybeAdminBotProfile <- dataService.slackBotProfiles.allForSlackTeamId(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID).map(_.headOption)
      maybeClient <- Future.successful(maybeAdminBotProfile.map(slackEventService.clientFor))
      maybeLinkedAccount <- dataService.linkedAccounts.maybeForSlackFor(user)
      isAdmin <- (for {
        client <- maybeClient
        linkedAccount <- maybeLinkedAccount
      } yield {
        slackEventService.maybeSlackUserDataFor(linkedAccount.loginInfo.providerKey, client, (_) => None).map { maybeSlackUserData =>
          maybeSlackUserData.exists(_.accountTeamIds.contains(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID))
        }
      }).getOrElse(Future.successful(false))
    } yield isAdmin
  }

  def userDataFor(user: User, team: Team): Future[UserData] = {
    if (user.teamId != team.id) {
      for {
        isAdmin <- isAdmin(user)
        hasNoSlackLinkedAccount <- if (!isAdmin) {
          dataService.linkedAccounts.maybeForSlackFor(user).map(_.isEmpty)
        } else {
          Future.successful(false)
        }
      } yield {
        if (isAdmin) {
          UserData.asAdmin(user.id)
        } else {
          if (hasNoSlackLinkedAccount) {
            Logger.warn(s"User data requested but no Slack linked account exists for user ID ${user.id} with team ID ${user.teamId}")
          } else {
            Logger.error(s"Non-admin user data requested with mismatched team ID: user ID ${user.id} with team ID ${user.teamId} compared to requested team ID ${team.id}")
          }
          UserData.withoutProfile(user.id)
        }
      }
    } else {
      for {
        maybeSlackUserData <- maybeSlackUserDataFor(user, team)
        maybeMSAADUser <- maybeMSAADUserFor(user, team)
      } yield {
        userDataFor(user, team, maybeSlackUserData, maybeMSAADUser)
      }
    }
  }

  private def userDataFor(user: User, team: Team, maybeSlackUserData: Option[SlackUserData], maybeMSAADUser: Option[MSAADUser]): UserData = {
    maybeSlackUserData.map(d => UserData.fromSlackUserData(user, d)).getOrElse {
      maybeMSAADUser.map(d => UserData.fromMSAADUser(user, d)).getOrElse {
        UserData.withoutProfile(user.id)
      }
    }
  }

  private def maybeSlackUserDataFor(user: User, team: Team): Future[Option[SlackUserData]] = {
    for {
      slackBotProfiles <- dataService.slackBotProfiles.allFor(team)
      maybeSlackAccount <- dataService.linkedAccounts.maybeForSlackFor(user)
      maybeUserData <- maybeSlackAccount.map { slackAccount =>
        val slackUserId = slackAccount.loginInfo.providerKey
        val slackTeamIds = slackBotProfiles.map(_.slackTeamId)
        val maybeClient = slackBotProfiles.headOption.map(slackApiService.clientFor)
        maybeClient.map { client =>
          slackEventService.maybeSlackUserDataFor(slackUserId, client, (e) => {
            Logger.error(
              s"""Slack API reported user not found while trying to build user data for an Ellipsis user.
                 |Ellipsis user ID: ${user.id}
                 |Ellipsis user’s team ID: ${user.teamId}
                 |Ellipsis team ID for this requeust: ${team.id}
                 |Slack user ID: $slackUserId
                 |Slack team IDs for this team’s bot: ${slackTeamIds.mkString(",")}
             """.stripMargin, e)
            None
          })
        }.getOrElse(Future.successful(None))
      }.getOrElse(Future.successful(None))
    } yield maybeUserData
  }

  def maybeUserDataForEmail(email: String, team: Team): Future[Option[UserData]] = {
    // TODO: Slack-specific for now; in future we might want to lookup users from other sources
    for {
      maybeSlackBotProfile <- dataService.slackBotProfiles.allFor(team).map(_.headOption)
      maybeSlackUserData <- maybeSlackBotProfile.map { profile =>
        val client = SlackApiClient(profile, services, actorSystem, ec)
        slackEventService.maybeSlackUserDataForEmail(email, client)
      }.getOrElse(Future.successful(None))
      maybeUserData <- maybeSlackUserData.map { slackUserData =>
        for {
          user <- ensureUserFor(LoginInfo(Conversation.SLACK_CONTEXT, slackUserData.accountId), team.id)
        } yield {
          maybeSlackUserData.map(d => UserData.fromSlackUserData(user, d))
        }
      }.getOrElse(Future.successful(None))
    } yield maybeUserData
  }

  def maybeSlackUserDataFor(user: User): Future[Option[SlackUserData]] = {
    for {
      maybeTeam <- dataService.teams.find(user.teamId)
      maybeSlackUserData <- maybeTeam.map { team =>
        maybeSlackUserDataFor(user, team)
      }.getOrElse(Future.successful(None))
    } yield maybeSlackUserData
  }

  def maybeSlackTeamIdsFor(user: User): Future[Option[SlackUserTeamIds]] = {
    maybeSlackUserDataFor(user).map(_.map(_.accountTeamIds))
  }

  def maybeSlackProfileFor(user: User): Future[Option[SlackProfile]] = {
    for {
      maybeLinkedAccount <- dataService.linkedAccounts.maybeForSlackFor(user)
      maybeSlackUserData <- maybeSlackUserDataFor(user)
    } yield {
      for {
        linkedAccount <- maybeLinkedAccount
        slackTeamIds <- maybeSlackUserData.map(_.accountTeamIds)
      } yield SlackProfile(slackTeamIds, linkedAccount.loginInfo, maybeSlackUserData.flatMap(_.accountEnterpriseId))
    }
  }

  private def fetchMSAADUserFor(user: User, team: Team): String => Future[Option[MSAADUser]] = {
    _ => {
      for {
        botProfiles <- dataService.msTeamsBotProfiles.allFor(team.id)
        maybeLinkedAccount <- dataService.linkedAccounts.maybeForMSAzureActiveDirectoryFor(user)
        maybeUser <- maybeLinkedAccount.map { linked =>
          val userIdForContext = linked.loginInfo.providerKey
          val maybeClient = botProfiles.headOption.map(msTeamsApiService.profileClientFor)
          maybeClient.map { client =>
            client.getUserInfo(userIdForContext)
          }.getOrElse(Future.successful(None))
        }.getOrElse(Future.successful(None))
      } yield maybeUser
    }
  }

  private def maybeMSAADUserFor(user: User, team: Team): Future[Option[MSAADUser]] = {
    cacheService.getMSAADUser(user.id, fetchMSAADUserFor(user, team))
  }

  def maybeMSTeamsProfileFor(user: User): Future[Option[MSTeamsProfile]] = {
    for {
      maybeLinkedAccount <- dataService.linkedAccounts.maybeForMSTeamsFor(user)
      maybeBotProfile <- dataService.msTeamsBotProfiles.allFor(user.teamId).map(_.headOption)
    } yield {
      for {
        linkedAccount <- maybeLinkedAccount
        botProfile <- maybeBotProfile
      } yield MSTeamsProfile(botProfile.teamIdForContext, linkedAccount.loginInfo)
    }
  }

  def findForInvocationToken(tokenId: String): Future[Option[User]] = {
    for {
      maybeToken <- dataService.invocationTokens.findNotExpired(tokenId)
      maybeUser <- maybeToken.map { token =>
        find(token.userId)
      }.getOrElse {
        if (configuration.getOptional[String]("application.version").contains("Development")) {
          // in dev, if not found, we assume the tokenId is a user ID
          find(tokenId)
        } else {
          Future.successful(None)
        }
      }
    } yield maybeUser
  }

  def allFor(team: Team): Future[Seq[User]] = {
    val action = allForQuery(team.id).result
    dataService.run(action)
  }

}
