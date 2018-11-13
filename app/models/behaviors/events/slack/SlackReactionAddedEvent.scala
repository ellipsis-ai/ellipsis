package models.behaviors.events.slack

import models.behaviors.BehaviorResponse
import models.behaviors.behavior.Behavior
import models.behaviors.events.{Event, EventType, MessageUserData, SlackEventContext}
import models.team.Team
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

case class SlackReactionAddedEvent(
                                    eventContext: SlackEventContext,
                                    reaction: String,
                                    maybeMessage: Option[SlackMessage]
                                  ) extends Event {

  override type EC = SlackEventContext

  val eventType: EventType = EventType.chat

  override val isEphemeral: Boolean = false

  lazy val messageText: String = maybeMessage.map(_.originalText).getOrElse("")
  lazy val invocationLogText: String = relevantMessageText

  val maybeOriginalEventType: Option[EventType] = None

  override val isResponseExpected: Boolean = false
  val includesBotMention: Boolean = true

  override val maybeReactionAdded: Option[String] = Some(reaction)

  val maybeMessageIdForReaction: Option[String] = None

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               services: DefaultServices
                             )(implicit ec: ExecutionContext): Future[Seq[BehaviorResponse]] = {
    val dataService = services.dataService
    for {
      possibleActivatedTriggers <- dataService.behaviorGroupDeployments.possibleActivatedTriggersFor(this, maybeTeam, maybeChannel, eventContext.name, maybeLimitToBehavior)
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
            userExpectsResponse = true
          )
        } yield response
      })
    } yield responses
  }


  def messageUserDataList: Set[MessageUserData] = {
    maybeMessage.map { message =>
      message.userList.map(MessageUserData.fromSlackUserData)
    }.getOrElse(Set())
  }

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = this

}
