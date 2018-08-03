package models.behaviors.conversations

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.behaviors.BotResult
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.conversations.parentconversation.NewParentConversation
import models.behaviors.events.{Event, EventType, SlackEvent, SlackMessageEvent}
import models.behaviors.triggers.messagetrigger.MessageTrigger
import services.caching.CacheService
import services.{DataService, DefaultServices}
import slick.dbio.DBIO
import utils.SlackMessageReactionHandler

import scala.concurrent.{ExecutionContext, Future}

case class InvokeBehaviorConversation(
                                       id: String,
                                       behaviorVersion: BehaviorVersion,
                                       maybeTrigger: Option[MessageTrigger],
                                       maybeTriggerMessage: Option[String],
                                       context: String, // Slack, etc
                                       maybeChannel: Option[String],
                                       maybeThreadId: Option[String],
                                       userIdForContext: String, // id for Slack, etc user
                                       maybeTeamIdForContext: Option[String],
                                       startedAt: OffsetDateTime,
                                       maybeLastInteractionAt: Option[OffsetDateTime],
                                       state: String = Conversation.NEW_STATE,
                                       maybeScheduledMessageId: Option[String],
                                       maybeOriginalEventType: Option[EventType],
                                       maybeParentId: Option[String]
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

  def updateStateToAction(newState: String, dataService: DataService): DBIO[Conversation] = {
    dataService.conversations.saveAction(this.copy(state = newState))
  }

  def paramStateIn(collectionStates: Seq[CollectionState]): ParamCollectionState = {
    collectionStates.flatMap {
      case s: ParamCollectionState => Some(s)
      case _ => None
    }.head // There should always be a match
  }

  def collectionStatesFor(event: Event, services: DefaultServices)(implicit ec: ExecutionContext): Future[Seq[CollectionState]] = {
    services.dataService.run(collectionStatesForAction(event, services))
  }

  def collectionStatesForAction(event: Event, services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Seq[CollectionState]] = {
    for {
      user <- event.ensureUserAction(services.dataService)
      simpleTokenState <- SimpleTokenCollectionState.fromAction(user, this, event, services)
      paramState <- ParamCollectionState.fromAction(this, event, services)
    } yield Seq(simpleTokenState, paramState)
  }

  def updateToNextStateAction(event: Event, services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Conversation] = {
    for {
      collectionStates <- collectionStatesForAction(event, services)
      collectionStatesWithIsComplete <- DBIO.sequence(collectionStates.map { collectionState =>
        collectionState.isCompleteInAction(this).map { isComplete => (collectionState, isComplete) }
      })
      updated <- {
        val targetState =
          collectionStatesWithIsComplete.
            find { case(_, isComplete) => !isComplete }.
            map { case(collectionState, _) => collectionState.name }.
            getOrElse(Conversation.DONE_STATE)
        updateStateToAction(targetState, services.dataService)
      }
    } yield updated
  }

  def updateWithAction(event: Event, services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Conversation] = {

    for {
      collectionStates <- collectionStatesForAction(event, services)
      updated <- collectionStates.find(_.name == state).map(_.collectValueFromAction(this)).getOrElse {
        state match {
          case Conversation.NEW_STATE => updateToNextStateAction(event, services)
          case Conversation.DONE_STATE => DBIO.successful(this)
        }
      }
    } yield updated
  }

  def respondAction(
                     event: Event,
                     isReminding: Boolean,
                     services: DefaultServices
                   )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    for {
      collectionStates <- collectionStatesForAction(event, services)
      result <- collectionStates.find(_.name == state).map(_.promptResultForAction(this, isReminding)).getOrElse {
        val paramState = paramStateIn(collectionStates)
        services.dataService.behaviorResponses.buildForAction(event, behaviorVersion, paramState.invocationMap, maybeTrigger, Some(this), None).flatMap { br =>
          br.resultForFilledOutAction
        }
      }
    } yield result
  }

  def respond(
               event: Event,
               isReminding: Boolean,
               services: DefaultServices
             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val eventualResponse = services.dataService.run(respondAction(event, isReminding, services))
    event match {
      case event: SlackMessageEvent => {
        implicit val actorSystem = services.actorSystem
        SlackMessageReactionHandler.handle(services.slackApiService.clientFor(event.profile), eventualResponse, event.channel, event.ts)
      }
      case _ =>
    }
    eventualResponse
  }

  def maybeNextParamToCollectAction(
                               event: Event,
                               services: DefaultServices
                     )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Option[BehaviorParameter]] = {
    for {
      collectionStates <- collectionStatesForAction(event, services)
      maybeCollectionState <- DBIO.successful(collectionStates.find(_.name == state))
      maybeParam <- maybeCollectionState.map {
        case s: ParamCollectionState => s.maybeNextToCollectAction(this)
        case _ => DBIO.successful(None)
      }.getOrElse(DBIO.successful(None))
    } yield maybeParam.map(_._1)
  }

  def maybeNextParamToCollect(
                               event: Event,
                               services: DefaultServices
                             )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BehaviorParameter]] = {
    services.dataService.run(maybeNextParamToCollectAction(event, services))
  }

}

object InvokeBehaviorConversation {

  val COLLECT_SIMPLE_TOKENS_STATE = "collect_simple_tokens"
  val COLLECT_PARAM_VALUES_STATE = "collect_param_values"

  val statesRequiringPrivateMessage = Seq(
    COLLECT_SIMPLE_TOKENS_STATE
  )

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 event: Event,
                 maybeChannel: Option[String],
                 maybeActivatedTrigger: Option[MessageTrigger],
                 maybeParent: Option[NewParentConversation],
                 dataService: DataService,
                 cacheService: CacheService
                 )(implicit ec: ExecutionContext): Future[InvokeBehaviorConversation] = {
    val maybeTeamIdForContext = event match {
      case e: SlackEvent => Some(e.userSlackTeamId)
      case _ => None
    }
    val action = for {
      maybeParent <- maybeParent.map { parent =>
        dataService.parentConversations.createAction(parent).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      newInstance <- DBIO.successful(InvokeBehaviorConversation(
        IDs.next,
        behaviorVersion,
        maybeActivatedTrigger,
        event.maybeMessageText,
        event.name,
        maybeChannel,
        None,
        event.userIdForContext,
        maybeTeamIdForContext,
        OffsetDateTime.now,
        None,
        Conversation.NEW_STATE,
        event.maybeScheduled.map(_.id),
        Some(event.originalEventType),
        maybeParent.map(_.id)
      ))
      _ <- dataService.conversations.saveAction(newInstance)
    } yield {
      maybeChannel.foreach { channel =>
        cacheService.cacheLastConversationId(event.teamId, channel, newInstance.id)
      }
      cacheService.cacheMessageUserDataList(event.messageUserDataList.toSeq, newInstance.id)
      newInstance
    }
    dataService.run(action.transactionally)
  }
}
