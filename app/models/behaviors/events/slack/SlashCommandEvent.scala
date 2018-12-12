package models.behaviors.events.slack

import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.slack.botprofile.SlackBotProfile
import models.behaviors.BehaviorResponse
import models.behaviors.behavior.Behavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.{Event, EventType, UserData, SlackEventContext}
import models.team.Team
import services.DefaultServices
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class SlashCommandEvent(
                              eventContext: SlackEventContext,
                              message: SlackMessage,
                              responseUrl: String
                            ) extends Event {

  override type EC = SlackEventContext

  val profile: SlackBotProfile = eventContext.profile
  val channel: String = eventContext.channel

  val eventType: EventType = EventType.chat

  override val isEphemeral: Boolean = true
  override val maybeResponseUrl: Option[String] = Some(responseUrl)

  lazy val messageText: String = message.originalText
  lazy val invocationLogText: String = relevantMessageText

  val maybeOriginalEventType: Option[EventType] = None

  override val isResponseExpected: Boolean = true
  val includesBotMention: Boolean = true

  override val beQuiet: Boolean = true

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


  def messageUserDataListAction(services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Set[UserData]] = {
    DBIO.sequence(message.userList.toSeq.map { data =>
      services.dataService.users.ensureUserForAction(LoginInfo(Conversation.SLACK_CONTEXT, data.accountId), Seq(), ellipsisTeamId).map { user =>
        UserData.fromSlackUserData(user, data)
      }
    }).map(_.toSet)
  }

  def withOriginalEventType(originalEventType: EventType, isUninterrupted: Boolean): Event = this

}
