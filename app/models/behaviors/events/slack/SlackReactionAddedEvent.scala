package models.behaviors.events.slack

import akka.actor.ActorSystem
import json.UserData
import models.behaviors.BehaviorResponse
import models.behaviors.behavior.Behavior
import models.behaviors.events.{Event, EventType, SlackEventContext}
import models.behaviors.scheduling.Scheduled
import models.team.Team
import services.DefaultServices
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class SlackReactionAddedEvent(
                                    eventContext: SlackEventContext,
                                    reaction: String,
                                    maybeMessage: Option[SlackMessage]
                                  ) extends Event {

  override val maybeScheduled: Option[Scheduled] = None

  override type EC = SlackEventContext

  val eventType: EventType = EventType.chat

  override val isEphemeral: Boolean = false

  lazy val messageText: String = maybeMessage.map(_.originalText).getOrElse("")
  lazy val invocationLogText: String = relevantMessageText

  val maybeOriginalEventType: Option[EventType] = None

  override def maybePermalinkFor(services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[String]] = {
    (for {
      msg <- maybeMessage
      ts <- msg.maybeTs
    } yield {
      eventContext.maybePermalinkFor(ts, services)
    }).getOrElse(Future.successful(None))
  }

  override val isResponseExpected: Boolean = false
  val includesBotMention: Boolean = true

  override val maybeReactionAdded: Option[String] = Some(reaction)

  val maybeMessageId: Option[String] = maybeMessage.flatMap(_.maybeTs)

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               services: DefaultServices
                             )(implicit ec: ExecutionContext): Future[Seq[BehaviorResponse]] = {
    val dataService = services.dataService
    for {
      possibleActivatedTriggers <- dataService.behaviorGroupDeployments.possibleActivatedTriggersFor(maybeTeam, maybeChannel, eventContext.name, maybeLimitToBehavior)
      activatedTriggers <- activatedTriggersIn(possibleActivatedTriggers, dataService)
      responses <- Future.sequence(activatedTriggers.map { trigger =>
        for {
          params <- dataService.behaviorParameters.allFor(trigger.behaviorVersion)
          response <- dataService.behaviorResponses.buildFor(
            this,
            trigger.behaviorVersion,
            trigger.invocationParamsFor(this, params),
            Some(trigger),
            None,
            None,
            userExpectsResponse = true,
            maybeMessageListener = None
          )
        } yield response
      })
    } yield responses
  }


  def messageUserDataListAction(services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Set[UserData]] = {
    maybeMessage.map { message =>
      UserData.allFromSlackUserDataListAction(message.userList, ellipsisTeamId, services)
    }.getOrElse(DBIO.successful(Set()))
  }

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = this
}
