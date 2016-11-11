package models.behaviors.conversations

import models.behaviors.{BotResult, SimpleTextResult}
import models.behaviors.behaviorparameter.{BehaviorParameter, BehaviorParameterContext}
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageEvent
import play.api.Configuration
import play.api.cache.CacheApi
import services.{AWSLambdaConstants, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class ParamCollectionState(
                                params: Seq[BehaviorParameter],
                                collected: Seq[CollectedParameterValue],
                                event: MessageEvent,
                                dataService: DataService,
                                cache: CacheApi,
                                configuration: Configuration
                               ) extends CollectionState {

  val name = InvokeBehaviorConversation.COLLECT_PARAM_VALUES_STATE

  val rankedParams = params.sortBy(_.rank)

  def maybeNextToCollect(conversation: Conversation): Future[Option[(BehaviorParameter, Option[CollectedParameterValue])]] = {
    val tuples = rankedParams.map { ea => (ea, collected.find(_.parameter == ea)) }

    val eventualWithHasValidValue = Future.sequence(tuples.map { case(param, maybeCollected) =>
      val context = BehaviorParameterContext(event, Some(conversation), param, cache, dataService, configuration)
      val eventualHasValidValue = maybeCollected.map { collected =>
        param.paramType.isValid(collected.valueString, context)
      }.getOrElse(Future.successful(false))

      eventualHasValidValue.map { hasValidValue =>
        (param, maybeCollected, hasValidValue)
      }
    })

    eventualWithHasValidValue.map { withHasValidValue =>
      withHasValidValue.
        find { case (param, maybeCollected, hasValidValue) => !hasValidValue }.
        map { case (param, maybeCollected, hasValidValue) => (param, maybeCollected) }
    }
  }

  def isCompleteIn(conversation: Conversation): Future[Boolean] = maybeNextToCollect(conversation).map(_.isEmpty)

  def invocationMap: Map[String, String] = {
    rankedParams.zipWithIndex.map { case(ea, i) =>
      val maybeParamValue = collected.find(_.parameter.id == ea.id).map(_.valueString)
      (AWSLambdaConstants.invocationParamFor(i), maybeParamValue.getOrElse(""))
    }.toMap
  }

  def collectValueFrom(conversation: InvokeBehaviorConversation): Future[Conversation] = {
    for {
      maybeNextToCollect <- maybeNextToCollect(conversation)
      updatedConversation <- maybeNextToCollect.map { case(param, maybeCollected) =>
        val context = BehaviorParameterContext(event, Some(conversation), param, cache, dataService, configuration)
        param.paramType.handleCollected(event, context).map(_ => conversation)
      }.getOrElse(Future.successful(conversation))
      updatedConversation <- updatedConversation.updateToNextState(event, cache, dataService, configuration)
    } yield updatedConversation
  }

  def promptResultFor(conversation: Conversation): Future[BotResult] = {
    for {
      maybeNextToCollect <- maybeNextToCollect(conversation)
      result <- maybeNextToCollect.map { case(param, maybeCollected) =>
        val context = BehaviorParameterContext(event, Some(conversation), param, cache, dataService, configuration)
        param.prompt(maybeCollected, context)
      }.getOrElse {
        Future.successful("All done!")
      }.map { prompt =>
        SimpleTextResult(prompt, conversation.behaviorVersion.forcePrivateResponse)
      }
    } yield result
  }

}

object ParamCollectionState {

  def from(
            conversation: Conversation,
            event: MessageEvent,
            dataService: DataService,
            cache: CacheApi,
            configuration: Configuration
          ): Future[ParamCollectionState] = {
    for {
      params <- dataService.behaviorParameters.allFor(conversation.behaviorVersion)
      collected <- dataService.collectedParameterValues.allFor(conversation)
    } yield ParamCollectionState(params, collected, event, dataService, cache, configuration)
  }

}
