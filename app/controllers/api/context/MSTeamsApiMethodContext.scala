package controllers.api.context

import akka.actor.ActorSystem
import controllers.api.APIResponder
import controllers.api.exceptions.InvalidTokenException
import models.accounts.ms_teams.botprofile.MSTeamsBotProfile
import models.accounts.ms_teams.profile.MSTeamsProfile
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events._
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.team.Team
import play.api.Logger
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class MSTeamsApiMethodContext(
                                    maybeInvocationToken: Option[InvocationToken],
                                    maybeUser: Option[User],
                                    botProfile: MSTeamsBotProfile,
                                    profile: MSTeamsProfile,
                                    maybeScheduledMessage: Option[ScheduledMessage],
                                    maybeTeam: Option[Team],
                                    isInvokedExternally: Boolean,
                                    services: DefaultServices,
                                    responder: APIResponder,
                                    implicit val ec: ExecutionContext,
                                    implicit val actorSystem: ActorSystem
                                ) extends ApiMethodContext {

  val mediumText: String = "MS Teams"

  def maybeMessageEventFor(message: String, channel: String, maybeOriginalEventType: Option[EventType], maybeMessageTs: Option[String]): Future[Option[Event]] = ???

  def runEventFor(
                   behaviorVersion: BehaviorVersion,
                   argumentsMap: Map[String, String],
                   channel: String,
                   maybeOriginalEventType: Option[EventType],
                   maybeTriggeringMessageId: Option[String]
                 ): Future[RunEvent] = ???

  def getToken: Future[String] = {
    val client = services.msTeamsApiService.profileClientFor(botProfile)
    client.fetchGraphApiToken
  }

  def printEventCreationError(): Unit = {
    Logger.error(
      s"""Event creation likely failed for API context:
         |
         |MS Teams teame ID: ${botProfile.teamIdForContext}
         |MS Teams user profile ID: ${profile.loginInfo.providerID}
         |""".stripMargin
    )
  }

}

object MSTeamsApiMethodContext {

  def maybeCreateFor(
                      token: String,
                      services: DefaultServices,
                      responder: APIResponder
                    )(implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Option[MSTeamsApiMethodContext]] = {
    val dataService = services.dataService
    for {
      maybeUserForApiToken <- dataService.apiTokens.maybeUserForApiToken(token)
      maybeInvocationToken <- dataService.invocationTokens.findNotExpired(token)
      maybeScheduledMessage <- maybeInvocationToken.flatMap { token =>
        token.maybeScheduledMessageId.map { msgId =>
          dataService.scheduledMessages.find(msgId)
        }
      }.getOrElse(Future.successful(None))
      maybeUserForInvocationToken <- dataService.users.findForInvocationToken(token)
      maybeUser <- Future.successful(maybeUserForApiToken.orElse(maybeUserForInvocationToken))
      maybeTeam <- maybeUser.map { user =>
        dataService.teams.find(user.teamId)
      }.getOrElse {
        throw new InvalidTokenException()
      }
      maybeProfile <- maybeUser.map { user =>
        dataService.users.maybeMSTeamsProfileFor(user)
      }.getOrElse(Future.successful(None))
      maybeTeamIdForContextForBot <- Future.successful {
        if (maybeUserForApiToken.isDefined) {
          maybeProfile.map(_.msTeamsUserId)
        } else {
          maybeInvocationToken.flatMap(_.maybeTeamIdForContext)
        }
      }
      maybeBotProfile <- maybeTeamIdForContextForBot.map { teamIdForContext =>
        dataService.msTeamsBotProfiles.find(teamIdForContext).map(_.headOption)
      }.getOrElse(Future.successful(None))
    } yield {
      for {
        botProfile <- maybeBotProfile
        slackProfile <- maybeProfile
      } yield {
        MSTeamsApiMethodContext(
          maybeInvocationToken,
          maybeUser,
          botProfile,
          slackProfile,
          maybeScheduledMessage,
          maybeTeam,
          isInvokedExternally = maybeUserForApiToken.isDefined,
          services,
          responder,
          ec,
          actorSystem
        )
      }
    }
  }
}
