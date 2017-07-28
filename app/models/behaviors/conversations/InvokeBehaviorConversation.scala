package models.behaviors.conversations

import java.time.OffsetDateTime

import models.IDs
import models.behaviors.BotResult
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.{Event, SlackMessageEvent}
import models.behaviors.triggers.messagetrigger.MessageTrigger
import models.behaviors.{BehaviorResponse, BotResult}
import services.{DataService, DefaultServices}
import slick.dbio.DBIO
import utils.SlackMessageReactionHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class InvokeBehaviorConversation(
                                       id: String,
                                       behaviorVersion: BehaviorVersion,
                                       maybeTrigger: Option[MessageTrigger],
                                       maybeTriggerMessage: Option[String],
                                       context: String, // Slack, etc
                                       maybeChannel: Option[String],
                                       maybeThreadId: Option[String],
                                       userIdForContext: String, // id for Slack, etc user
                                       startedAt: OffsetDateTime,
                                       maybeLastInteractionAt: Option[OffsetDateTime],
                                       state: String = Conversation.NEW_STATE,
                                       maybeScheduledMessageId: Option[String]
                                      ) extends Conversation {

  val conversationType = Conversation.INVOKE_BEHAVIOR

  def copyWithMaybeThreadId(maybeId: Option[String]): Conversation = {
    copy(maybeThreadId = maybeId)
  }

  def copyWithLastInteractionAt(dt: OffsetDateTime): Conversation = {
    copy(maybeLastInteractionAt = Some(dt))
  }

  override val stateRequiresPrivateMessage: Boolean = {
    InvokeBehaviorConversation.statesRequiringPrivateMessage.contains(state)
  }

  def updateStateTo(newState: String, dataService: DataService): Future[Conversation] = {
    dataService.conversations.save(this.copy(state = newState))
  }

  def paramStateIn(collectionStates: Seq[CollectionState]): ParamCollectionState = {
    collectionStates.flatMap {
      case s: ParamCollectionState => Some(s)
      case _ => None
    }.head // There should always be a match
  }

  def collectionStatesFor(event: Event, services: DefaultServices): Future[Seq[CollectionState]] = {
    services.dataService.run(collectionStatesForAction(event, services))
  }

  def collectionStatesForAction(event: Event, services: DefaultServices): DBIO[Seq[CollectionState]] = {
    for {
      user <- event.ensureUserAction(services.dataService)
      simpleTokenState <- SimpleTokenCollectionState.fromAction(user, this, event, services)
      userEnvVarState <- UserEnvVarCollectionState.fromAction(user, this, event, services)
      paramState <- ParamCollectionState.fromAction(this, event, services)
    } yield Seq(simpleTokenState, userEnvVarState, paramState)
  }

  def updateToNextState(event: Event, services: DefaultServices): Future[Conversation] = {
    for {
      collectionStates <- collectionStatesFor(event, services)
      collectionStatesWithIsComplete <- Future.sequence(collectionStates.map { collectionState =>
        collectionState.isCompleteIn(this).map { isComplete => (collectionState, isComplete) }
      })
      updated <- {
        val targetState =
          collectionStatesWithIsComplete.
            find { case(_, isComplete) => !isComplete }.
            map { case(collectionState, _) => collectionState.name }.
            getOrElse(Conversation.DONE_STATE)
        updateStateTo(targetState, services.dataService)
      }
    } yield updated
  }

  def updateWith(event: Event, services: DefaultServices): Future[Conversation] = {

    for {
      collectionStates <- collectionStatesFor(event, services)
      updated <- collectionStates.find(_.name == state).map(_.collectValueFrom(this)).getOrElse {
        state match {
          case Conversation.NEW_STATE => updateToNextState(event, services)
          case Conversation.DONE_STATE => Future.successful(this)
        }
      }
    } yield updated
  }

  def respondAction(
                     event: Event,
                     isReminding: Boolean,
                     services: DefaultServices
                   ): DBIO[BotResult] = {
    for {
      collectionStates <- collectionStatesForAction(event, services)
      result <- collectionStates.find(_.name == state).map(_.promptResultForAction(this, isReminding)).getOrElse {
        val paramState = paramStateIn(collectionStates)
        services.dataService.behaviorResponses.buildForAction(event, behaviorVersion, paramState.invocationMap, maybeTrigger, Some(this)).flatMap { br =>
          br.resultForFilledOutAction
        }
      }
    } yield result
  }

  def respond(
               event: Event,
               isReminding: Boolean,
               services: DefaultServices
             ): Future[BotResult] = {
    val eventualResponse = services.dataService.run(respondAction(event, isReminding, services))
    event match {
      case event: SlackMessageEvent => {
        implicit val actorSystem = services.actorSystem
        SlackMessageReactionHandler.handle(event.client, eventualResponse, event.channel, event.ts)
      }
      case _ =>
    }
    eventualResponse
  }

  def maybeNextParamToCollect(
                               event: Event,
                               services: DefaultServices
                     ): Future[Option[BehaviorParameter]] = {
    for {
      collectionStates <- collectionStatesFor(event, services)
      maybeCollectionState <- Future.successful(collectionStates.find(_.name == state))
      maybeParam <- maybeCollectionState.map {
        case s: ParamCollectionState => s.maybeNextToCollect(this)
        case _ => Future.successful(None)
      }.getOrElse(Future.successful(None))
    } yield maybeParam.map(_._1)
  }

}

object InvokeBehaviorConversation {

  val COLLECT_SIMPLE_TOKENS_STATE = "collect_simple_tokens"
  val COLLECT_USER_ENV_VARS_STATE = "collect_user_env_vars"
  val COLLECT_PARAM_VALUES_STATE = "collect_param_values"

  val statesRequiringPrivateMessage = Seq(
    COLLECT_SIMPLE_TOKENS_STATE,
    COLLECT_USER_ENV_VARS_STATE
  )

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 event: Event,
                 maybeChannel: Option[String],
                 maybeActivatedTrigger: Option[MessageTrigger],
                 dataService: DataService
                 ): Future[InvokeBehaviorConversation] = {
    val newInstance =
      InvokeBehaviorConversation(
        IDs.next,
        behaviorVersion,
        maybeActivatedTrigger,
        event.maybeMessageText,
        event.name,
        maybeChannel,
        None,
        event.userIdForContext,
        OffsetDateTime.now,
        None,
        Conversation.NEW_STATE,
        event.maybeScheduled.map(_.id)
      )
    dataService.conversations.save(newInstance).map(_ => newInstance)
  }
}
