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
import models.accounts.slack.profile.SlackProfile
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.{Event, SlackMessageEvent}
import models.team.Team
import play.api.Logger
import play.api.libs.json.Json
import services.DefaultServices
import services.slack.SlackApiClient
import json.Formatting._

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
      case slackEvent: SlackMessageEvent => dataService.linkedAccounts.find(LoginInfo(slackEvent.name, slackEvent.user), team.id).map { maybeLinked =>
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

  def ensureUserForAction(loginInfo: LoginInfo, teamId: String): DBIO[User] = {
    maybeExistingUserForAction(loginInfo, teamId).flatMap { maybeExisting =>
      maybeExisting.map(DBIO.successful).getOrElse {
        createNewUserAction(loginInfo, teamId)
      }
    }
  }

  def ensureUserFor(loginInfo: LoginInfo, teamId: String): Future[User] = {
    dataService.run(ensureUserForAction(loginInfo, teamId))
  }

  def teamAccessForAction(user: User, maybeTargetTeamId: Option[String]): DBIO[UserTeamAccess] = {
    for {
      loggedInTeam <- dataService.teams.findAction(user.teamId).map(_.get)
      isAdmin <- DBIO.from(isAdmin(user))
      maybeTeam <- maybeTargetTeamId.map { targetTeamId =>
        if (targetTeamId != user.teamId && !isAdmin) {
          DBIO.successful(None)
        } else {
          dataService.teams.findAction(targetTeamId)
        }
      }.getOrElse {
        dataService.teams.findAction(user.teamId)
      }
      maybeSlackBotProfile <- maybeTeam.map { team =>
        dataService.slackBotProfiles.allForAction(team).map(_.headOption)
      }.getOrElse(DBIO.successful(None))
      maybeBotName <- maybeSlackBotProfile.map { slackBotProfile =>
        DBIO.from(dataService.slackBotProfiles.maybeNameFor(slackBotProfile))
      }.getOrElse(DBIO.successful(None))
    } yield UserTeamAccess(user, loggedInTeam, maybeTeam, maybeBotName, maybeTeam.exists(t => t.id != user.teamId))
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
        slackEventService.maybeSlackUserDataFor(linkedAccount.loginInfo.providerKey, LinkedAccount.ELLIPSIS_SLACK_TEAM_ID, client, (_) => None).map { maybeSlackUserData =>
          maybeSlackUserData.exists(_.accountTeamId == LinkedAccount.ELLIPSIS_SLACK_TEAM_ID)
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
        maybeTeam <- dataService.teams.find(user.teamId)
      } yield {
        if (isAdmin) {
          UserData.asAdmin(user.id)
        } else {
          if (hasNoSlackLinkedAccount) {
            Logger.warn(s"User data requested but no Slack linked account exists for user ID ${user.id} with team ID ${user.teamId}")
          } else {
            Logger.error(s"Non-admin user data requested with mismatched team ID: user ID ${user.id} with team ID ${user.teamId} compared to requested team ID ${team.id}")
          }
          UserData.withoutProfile(user.id, maybeTeam)
        }
      }
    } else {
      for {
        maybeSlackUserData <- maybeSlackUserDataFor(user, team)
      } yield {
        userDataFor(user, team, maybeSlackUserData)
      }
    }
  }

  private def userDataFor(user: User, team: Team, maybeSlackUserData: Option[SlackUserData]): UserData = {
    val maybeTzString = maybeSlackUserData.flatMap(_.tz).orElse(team.maybeTimeZone.map(_.toString))
    UserData(
      id = user.id,
      userName = maybeSlackUserData.map(_.getDisplayName),
      fullName = maybeSlackUserData.flatMap(_.maybeRealName),
      tz = maybeTzString,
      teamName = Some(team.name),
      email = maybeSlackUserData.flatMap(_.profile.flatMap(_.email)),
      context = if (maybeSlackUserData.isDefined) Some(Conversation.SLACK_CONTEXT) else None,
      userIdForContext = maybeSlackUserData.map(_.accountId),
      details = maybeSlackUserData.map(Json.toJsObject(_))
    )
  }

  private def maybeSlackUserDataFor(user: User, team: Team): Future[Option[SlackUserData]] = {
    for {
      maybeSlackBotProfile <- dataService.slackBotProfiles.allFor(team).map(_.headOption)
      maybeSlackAccount <- dataService.linkedAccounts.maybeForSlackFor(user)
      maybeUserData <- (for {
        slackBotProfile <- maybeSlackBotProfile
        slackAccount <- maybeSlackAccount
      } yield {
        val slackUserId = slackAccount.loginInfo.providerKey
        val slackTeamId = slackBotProfile.slackTeamId
        slackEventService.maybeSlackUserDataFor(slackUserId, slackTeamId, slackApiService.clientFor(slackBotProfile), (e) => {
          Logger.error(
            s"""Slack API reported user not found while trying to build user data for an Ellipsis user.
               |Ellipsis user ID: ${user.id}
               |Ellipsis user’s team ID: ${user.teamId}
               |Ellipsis team ID for this requeust: ${team.id}
               |Slack user ID: $slackUserId
               |Slack team ID for this team’s bot: $slackTeamId
             """.stripMargin, e)
          None
        })
      }).getOrElse(Future.successful(None))
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
          Some(userDataFor(user, team, maybeSlackUserData))
        }
      }.getOrElse(Future.successful(None))
    } yield maybeUserData
  }

  def maybeSlackTeamIdFor(user: User): Future[Option[String]] = {
    for {
      maybeTeam <- dataService.teams.find(user.teamId)
      maybeSlackUserData <- maybeTeam.map { team =>
        maybeSlackUserDataFor(user, team)
      }.getOrElse(Future.successful(None))
    } yield maybeSlackUserData.map(_.accountTeamId)
  }

  def maybeSlackProfileFor(user: User): Future[Option[SlackProfile]] = {
    for {
      maybeLinkedAccount <- dataService.linkedAccounts.maybeForSlackFor(user)
      maybeSlackTeamId <- maybeSlackTeamIdFor(user)
    } yield {
      for {
        linkedAccount <- maybeLinkedAccount
        slackTeamId <- maybeSlackTeamId
      } yield SlackProfile(slackTeamId, linkedAccount.loginInfo)
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
