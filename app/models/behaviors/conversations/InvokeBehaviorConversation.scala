package models.behaviors.conversations

import java.time.OffsetDateTime

import models.IDs
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.{BehaviorResponse, BotResult}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.triggers.messagetrigger.MessageTrigger
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

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

  def collectionStatesFor(event: Event, dataService: DataService, cache: CacheApi, configuration: Configuration): Future[Seq[CollectionState]] = {
    for {
      user <- event.ensureUser(dataService)
      simpleTokenState <- SimpleTokenCollectionState.from(user, this, event, dataService, cache, configuration)
      userEnvVarState <- UserEnvVarCollectionState.from(user, this, event, dataService, cache, configuration)
      paramState <- ParamCollectionState.from(this, event, dataService, cache, configuration)
    } yield Seq(simpleTokenState, userEnvVarState, paramState)
  }

  def updateToNextState(event: Event, cache: CacheApi, dataService: DataService, configuration: Configuration): Future[Conversation] = {
    for {
      collectionStates <- collectionStatesFor(event, dataService, cache, configuration)
      collectionStatesWithIsComplete <- Future.sequence(collectionStates.map { collectionState =>
        collectionState.isCompleteIn(this).map { isComplete => (collectionState, isComplete) }
      })
      updated <- {
        val targetState =
          collectionStatesWithIsComplete.
            find { case(_, isComplete) => !isComplete }.
            map { case(collectionState, _) => collectionState.name }.
            getOrElse(Conversation.DONE_STATE)
        updateStateTo(targetState, dataService)
      }
    } yield updated
  }

  def updateWith(event: Event, lambdaService: AWSLambdaService, dataService: DataService, cache: CacheApi, configuration: Configuration): Future[Conversation] = {

    for {
      collectionStates <- collectionStatesFor(event, dataService, cache, configuration)
      updated <- collectionStates.find(_.name == state).map(_.collectValueFrom(this)).getOrElse {
        state match {
          case Conversation.NEW_STATE => updateToNextState(event, cache, dataService, configuration)
          case Conversation.DONE_STATE => Future.successful(this)
        }
      }
    } yield updated
  }

  def respond(
               event: Event,
               isReminding: Boolean,
               lambdaService: AWSLambdaService,
               dataService: DataService,
               cache: CacheApi,
               ws: WSClient,
               configuration: Configuration
             ): Future[BotResult] = {
    for {
      collectionStates <- collectionStatesFor(event, dataService, cache, configuration)
      result <- collectionStates.find(_.name == state).map(_.promptResultFor(this, isReminding)).getOrElse {
        val paramState = paramStateIn(collectionStates)
        BehaviorResponse.buildFor(event, behaviorVersion, paramState.invocationMap, maybeTrigger, Some(this), lambdaService, dataService, cache, ws, configuration).flatMap { br =>
          br.resultForFilledOut
        }
      }
    } yield result
  }

  def maybeNextParamToCollect(
                       event: Event,
                       lambdaService: AWSLambdaService,
                       dataService: DataService,
                       cache: CacheApi,
                       ws: WSClient,
                       configuration: Configuration
                     ): Future[Option[BehaviorParameter]] = {
    for {
      collectionStates <- collectionStatesFor(event, dataService, cache, configuration)
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
