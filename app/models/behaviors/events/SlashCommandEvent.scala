package models.behaviors.events

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.BehaviorResponseType
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.{ActionChoice, BehaviorResponse, DeveloperContext}
import models.team.Team
import play.api.Configuration
import services.{DataService, DefaultServices}
import utils.{SlackMessageSender, UploadFileSpec}

import scala.concurrent.{ExecutionContext, Future}

case class SlashCommandEvent(
                              eventContext: SlackEventContext,
                              message: SlackMessage,
                              responseUrl: String
                            ) extends Event {

  override type EC = SlackEventContext

  val profile: SlackBotProfile = eventContext.profile
  val channel: String = eventContext.channel
  val user: String = eventContext.user

  val eventType: EventType = EventType.chat

  override val isEphemeral: Boolean = true
  override val maybeResponseUrl: Option[String] = Some(responseUrl)

  val userIdForContext: String = user

  lazy val messageText: String = message.originalText
  lazy val invocationLogText: String = relevantMessageText

  val maybeOriginalEventType: Option[EventType] = None

  override val isResponseExpected: Boolean = true
  val includesBotMention: Boolean = true

  override val beQuiet: Boolean = true

  def allBehaviorResponsesFor(
                               maybeTeam: Option[Team],
                               maybeLimitToBehavior: Option[Behavior],
                               services: DefaultServices
                             )(implicit ec: ExecutionContext): Future[Seq[BehaviorResponse]] = {
    val dataService = services.dataService
    for {
      possibleActivatedTriggers <- dataService.behaviorGroupDeployments.possibleActivatedTriggersFor(this, maybeTeam, maybeChannel, context, maybeLimitToBehavior)
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
    message.userList.map(MessageUserData.fromSlackUserData)
  }

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = this

}
