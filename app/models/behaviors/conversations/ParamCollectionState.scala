package models.behaviors.conversations

import akka.actor.ActorSystem
import models.behaviors.behaviorparameter.{BehaviorParameter, BehaviorParameterContext}
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.savedanswer.SavedAnswer
import models.behaviors.{BotResult, SimpleTextResult}
import play.api.Configuration
import play.api.cache.CacheApi
import services.{AWSLambdaConstants, DataService}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ParamCollectionState(
                                 params: Seq[BehaviorParameter],
                                 collected: Seq[CollectedParameterValue],
                                 savedAnswers: Seq[SavedAnswer],
                                 event: Event,
                                 dataService: DataService,
                                 cache: CacheApi,
                                 configuration: Configuration,
                                 actorSystem: ActorSystem
                               ) extends CollectionState {

  val name = InvokeBehaviorConversation.COLLECT_PARAM_VALUES_STATE

  val rankedParams = params.sortBy(_.rank)

  def allLeftToCollect(conversation: Conversation): Future[Seq[(BehaviorParameter, Option[String])]] = {
    val tuples = rankedParams.map { ea =>
      (ea, collected.find(_.parameter == ea), savedAnswers.find(_.inputId == ea.input.inputId))
    }

    val eventualWithHasValidValue = Future.sequence(tuples.map { case(param, maybeCollected, maybeSaved) =>
      val context = BehaviorParameterContext(event, Some(conversation), param, cache, dataService, configuration, actorSystem)
      val maybeValue = maybeCollected.map(_.valueString).orElse(maybeSaved.map(_.valueString))
      val eventualHasValidValue = maybeValue.map { valueString =>
        param.paramType.isValid(valueString, context)
      }.getOrElse(Future.successful(false))

      eventualHasValidValue.map { hasValidValue =>
        (param, maybeValue, hasValidValue)
      }
    })

    eventualWithHasValidValue.map { withHasValidValue =>
      withHasValidValue.
        filter { case (param, maybeValue, hasValidValue) => !hasValidValue }.
        map { case (param, maybeValue, hasValidValue) => (param, maybeValue) }
    }
  }

  def maybeNextToCollect(conversation: Conversation): Future[Option[(BehaviorParameter, Option[String])]] = {
    allLeftToCollect(conversation).map(_.headOption)
  }

  def isCompleteIn(conversation: Conversation): Future[Boolean] = maybeNextToCollect(conversation).map(_.isEmpty)

  def invocationMap: Map[String, String] = {
    rankedParams.zipWithIndex.map { case(ea, i) =>
      val maybeParamValue = collected.find(_.parameter.id == ea.id).map(_.valueString).orElse {
        savedAnswers.find(_.inputId == ea.input.inputId).map(_.valueString)
      }
      (AWSLambdaConstants.invocationParamFor(i), maybeParamValue.getOrElse(""))
    }.toMap
  }

  def collectValueFrom(conversation: InvokeBehaviorConversation): Future[Conversation] = {
    for {
      maybeNextToCollect <- maybeNextToCollect(conversation)
      updatedConversation <- maybeNextToCollect.map { case(param, maybeValue) =>
        val context = BehaviorParameterContext(event, Some(conversation), param, cache, dataService, configuration, actorSystem)
        param.paramType.handleCollected(event, context).map(_ => conversation)
      }.getOrElse(Future.successful(conversation))
      updatedConversation <- updatedConversation.updateToNextState(event, cache, dataService, configuration, actorSystem)
    } yield updatedConversation
  }

  def promptResultForAction(conversation: Conversation, isReminding: Boolean): DBIO[BotResult] = {
    for {
      maybeNextToCollect <- DBIO.from(maybeNextToCollect(conversation))
      result <- maybeNextToCollect.map { case(param, maybeValue) =>
        val context = BehaviorParameterContext(event, Some(conversation), param, cache, dataService, configuration, actorSystem)
        param.promptAction(maybeValue, context, this, isReminding)
      }.getOrElse {
        DBIO.successful("All done!")
      }.map { prompt =>
        SimpleTextResult(event, Some(conversation), prompt, conversation.behaviorVersion.forcePrivateResponse)
      }
    } yield result
  }

}

object ParamCollectionState {

  def fromAction(
                  conversation: Conversation,
                  event: Event,
                  dataService: DataService,
                  cache: CacheApi,
                  configuration: Configuration,
                  actorSystem: ActorSystem
                ): DBIO[ParamCollectionState] = {
    for {
      params <- dataService.behaviorParameters.allForAction(conversation.behaviorVersion)
      collected <- dataService.collectedParameterValues.allForAction(conversation)
      user <- event.ensureUserAction(dataService)
      savedAnswers <- dataService.savedAnswers.allForAction(user, params)
    } yield ParamCollectionState(params, collected, savedAnswers, event, dataService, cache, configuration, actorSystem)
  }

  def from(
            conversation: Conversation,
            event: Event,
            dataService: DataService,
            cache: CacheApi,
            configuration: Configuration,
            actorSystem: ActorSystem
          ): Future[ParamCollectionState] = {
    dataService.run(fromAction(conversation, event, dataService, cache, configuration, actorSystem))
  }

}
