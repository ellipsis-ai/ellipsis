package controllers.api.context

import akka.actor.ActorSystem
import controllers.api.APIResponder
import controllers.api.exceptions.InvalidTokenException
import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.{Event, EventType, ScheduledMessageTestEvent, TestEventContext}
import models.behaviors.invocationtoken.InvocationToken
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.behaviors.testing.{TestMessageEvent, TestRunEvent}
import models.team.Team
import play.api.Logger
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class NoMediumApiMethodContext(
                                     maybeInvocationToken: Option[InvocationToken],
                                     user: User,
                                     team: Team,
                                     maybeScheduledMessage: Option[ScheduledMessage],
                                     isInvokedExternally: Boolean,
                                     services: DefaultServices,
                                     responder: APIResponder,
                                     implicit val ec: ExecutionContext,
                                     implicit val actorSystem: ActorSystem
                                   ) extends ApiMethodContext {
  val maybeUser: Option[User] = Some(user)
  val maybeTeam: Option[Team] = Some(team)
  val mediumText: String = "requests with no target medium"

  val requiresChannel: Boolean = false

  def getFileFetchToken: Future[String] = Future.successful("no medium, no token")

  def maybeBaseMessageEventFor(message: String, maybeChannel: Option[String], maybeOriginalEventType: Option[EventType]): Future[Option[TestMessageEvent]] = {
    Future.successful(Some(TestMessageEvent(TestEventContext(user, team), message, includesBotMention = true)))
  }

  def maybeMessageEventFor(
                            message: String,
                            maybeChannel: Option[String],
                            maybeOriginalEventType: Option[EventType],
                            maybeOriginalMessageId: Option[String],
                            maybeOriginalMessageThreadId: Option[String]
                          ): Future[Option[Event]] = {
    maybeBaseMessageEventFor(message, maybeChannel, maybeOriginalEventType).map { maybeBaseEvent =>
      maybeBaseEvent.map { messageEvent =>
        val event: Event = maybeScheduledMessage.map { scheduledMessage =>
          ScheduledMessageTestEvent(messageEvent, scheduledMessage)
        }.getOrElse(messageEvent)
        event
      }
    }
  }

  def maybeRunEventFor(
                   behaviorVersion: BehaviorVersion,
                   argumentsMap: Map[String, String],
                   maybeChannel: Option[String],
                   eventType: EventType,
                   maybeOriginalEventType: Option[EventType],
                   maybeTriggeringMessageId: Option[String],
                   maybeTriggeringMessageThreadId: Option[String]
                 ): Future[Option[TestRunEvent]] = {
    Future.successful(Some(
      TestRunEvent(
        TestEventContext(user, team),
        behaviorVersion,
        argumentsMap
      )
    ))
  }

  def printEventCreationError(): Unit = {
    Logger.error(
      s"""Event creation likely failed for API no-medium context:
         |
         |User ID: ${user.id}
         |Team ID: ${team.id}
         |""".stripMargin
    )
  }
}

object NoMediumApiMethodContext {

  def maybeCreateFor(
                      token: String,
                      services: DefaultServices,
                      responder: APIResponder
                    )(implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[Option[NoMediumApiMethodContext]] = {
    val dataService = services.dataService
    for {
      maybeUserForApiToken <- dataService.apiTokens.maybeUserForApiToken(token)
      maybeInvocationToken <- dataService.invocationTokens.findNotExpired(token)
      maybeUserForInvocationToken <- dataService.users.findForInvocationToken(token)
      maybeUser <- Future.successful(maybeUserForApiToken.orElse(maybeUserForInvocationToken))
      maybeTeam <- maybeUser.map { user =>
        dataService.teams.find(user.teamId)
      }.getOrElse {
        throw new InvalidTokenException()
      }
      maybeScheduledMessage <- maybeInvocationToken.flatMap { token =>
        token.maybeScheduledMessageId.map { msgId =>
          dataService.scheduledMessages.find(msgId)
        }
      }.getOrElse(Future.successful(None))
    } yield {
      for {
        user <- maybeUser
        team <- maybeTeam
      } yield {
        NoMediumApiMethodContext(
          maybeInvocationToken,
          user,
          team,
          maybeScheduledMessage,
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
