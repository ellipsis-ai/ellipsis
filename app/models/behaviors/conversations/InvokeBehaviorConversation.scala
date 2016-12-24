package models.behaviors.conversations

import models.IDs
import models.behaviors._
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.triggers.messagetrigger.MessageTrigger
import org.joda.time.DateTime
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.slack.NewMessageEvent
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class InvokeBehaviorConversation(
                                      id: String,
                                      trigger: MessageTrigger,
                                      context: String, // Slack, etc
                                      userIdForContext: String, // id for Slack, etc user
                                      startedAt: DateTime,
                                      state: String = Conversation.NEW_STATE
                                      ) extends Conversation {

  val conversationType = Conversation.INVOKE_BEHAVIOR

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

  def collectionStatesFor(event: NewMessageEvent, dataService: DataService, cache: CacheApi, configuration: Configuration): Future[Seq[CollectionState]] = {
    for {
      user <- event.ensureUser(dataService)
      simpleTokenState <- SimpleTokenCollectionState.from(user, this, event, dataService, cache, configuration)
      userEnvVarState <- UserEnvVarCollectionState.from(user, this, event, dataService, cache, configuration)
      paramState <- ParamCollectionState.from(this, event, dataService, cache, configuration)
    } yield Seq(simpleTokenState, userEnvVarState, paramState)
  }

  def updateToNextState(event: NewMessageEvent, cache: CacheApi, dataService: DataService, configuration: Configuration): Future[Conversation] = {
    for {
      collectionStates <- collectionStatesFor(event, dataService, cache, configuration)
      collectionStatesWithIsComplete <- Future.sequence(collectionStates.map { collectionState =>
        collectionState.isCompleteIn(this).map { isComplete => (collectionState, isComplete) }
      })
      updated <- {
        val targetState =
          collectionStatesWithIsComplete.
            find { case(collectionState, isComplete) => !isComplete }.
            map { case(collectionState, _) => collectionState.name }.
            getOrElse(Conversation.DONE_STATE)
        updateStateTo(targetState, dataService)
      }
    } yield updated
  }

  def updateWith(event: NewMessageEvent, lambdaService: AWSLambdaService, dataService: DataService, cache: CacheApi, configuration: Configuration): Future[Conversation] = {
    import Conversation._

    for {
      collectionStates <- collectionStatesFor(event, dataService, cache, configuration)
      updated <- collectionStates.find(_.name == state).map(_.collectValueFrom(this)).getOrElse {
        state match {
          case NEW_STATE => updateToNextState(event, cache, dataService, configuration)
          case DONE_STATE => Future.successful(this)
        }
      }
    } yield updated
  }

  def respond(
               event: NewMessageEvent,
               lambdaService: AWSLambdaService,
               dataService: DataService,
               cache: CacheApi,
               ws: WSClient,
               configuration: Configuration
             ): Future[BotResult] = {
    for {
      collectionStates <- collectionStatesFor(event, dataService, cache, configuration)
      result <- collectionStates.find(_.name == state).map(_.promptResultFor(this)).getOrElse {
        val paramState = paramStateIn(collectionStates)
        BehaviorResponse.buildFor(event, behaviorVersion, paramState.invocationMap, trigger, Some(this), lambdaService, dataService, cache, ws, configuration).flatMap { br =>
          br.resultForFilledOut
        }
      }
    } yield result
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
                 context: String,
                 userIdForContext: String,
                 activatedTrigger: MessageTrigger,
                 dataService: DataService
                 ): Future[InvokeBehaviorConversation] = {
    val newInstance =
      InvokeBehaviorConversation(
        IDs.next,
        activatedTrigger,
        context,
        userIdForContext,
        DateTime.now,
        Conversation.NEW_STATE
      )
    dataService.conversations.save(newInstance).map(_ => newInstance)
  }
}
