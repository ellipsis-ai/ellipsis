package models.behaviors.conversations

import akka.actor.ActorSystem
import models.behaviors.behaviorparameter.{BehaviorParameter, BehaviorParameterContext}
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.savedanswer.SavedAnswer
import models.behaviors.{BotResult, SimpleTextResult}
import services.{AWSLambdaConstants, DefaultServices}
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class ParamCollectionState(
                                 params: Seq[BehaviorParameter],
                                 collected: Seq[CollectedParameterValue],
                                 savedAnswers: Seq[SavedAnswer],
                                 event: Event,
                                 services: DefaultServices
                               ) extends CollectionState {

  val name = Conversation.COLLECT_PARAM_VALUES_STATE

  val rankedParams = params.sortBy(_.rank)

  def allLeftToCollectAction(conversation: Conversation)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Seq[(BehaviorParameter, Option[String])]] = {
    val tuples = rankedParams.map { ea =>
      (ea, collected.find(_.parameterId == ea.id), savedAnswers.find(_.inputId == ea.input.inputId))
    }

    val eventualWithHasValidValue = DBIO.sequence(tuples.map { case(param, maybeCollected, maybeSaved) =>
      val paramContext = BehaviorParameterContext(event, Some(conversation), param, services)
      val maybeValue = maybeCollected.map(_.valueString).orElse(maybeSaved.map(_.valueString))
      val eventualHasValidValue = maybeValue.map { valueString =>
        param.paramType.isValidAction(valueString, paramContext)
      }.getOrElse(DBIO.successful(false))

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

  def maybeNextToCollectAction(conversation: Conversation)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Option[(BehaviorParameter, Option[String])]] = {
    allLeftToCollectAction(conversation).map(_.headOption)
  }

  def isCompleteInAction(conversation: Conversation)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Boolean] = maybeNextToCollectAction(conversation).map(_.isEmpty)

  def invocationMap: Map[String, String] = {
    rankedParams.zipWithIndex.map { case(ea, i) =>
      val maybeParamValue = collected.find(_.parameterId == ea.id).map(_.valueString).orElse {
        savedAnswers.find(_.inputId == ea.input.inputId).map(_.valueString)
      }
      (AWSLambdaConstants.invocationParamFor(i), maybeParamValue.getOrElse(""))
    }.toMap
  }

  def collectValueFromAction(conversation: InvokeBehaviorConversation)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[Conversation] = {
    for {
      maybeNextToCollect <- maybeNextToCollectAction(conversation)
      _ <- maybeNextToCollect.map { case(param, maybeValue) =>
        val context = BehaviorParameterContext(event, Some(conversation), param, services)
        param.paramType.handleCollectedAction(event, this, context)
      }.orElse {
        collected.reverse.headOption.map(_.parameterId).flatMap(lastCollectedParamId => params.find(_.id == lastCollectedParamId)).map { lastCollectedParam =>
          val context = BehaviorParameterContext(event, Some(conversation), lastCollectedParam, services)
          lastCollectedParam.paramType.handleCollectedAction(event, this, context)
        }
      }.getOrElse(DBIO.successful({}))
      updatedConversation <- conversation.updateToNextStateAction(event, services)
    } yield updatedConversation
  }

  def promptResultForAction(conversation: Conversation, isReminding: Boolean)(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
    for {
      maybeNextToCollect <- maybeNextToCollectAction(conversation)
      result <- maybeNextToCollect.map { case(param, maybeValue) =>
        val paramContext = BehaviorParameterContext(event, Some(conversation), param, services)
        param.promptResultAction(maybeValue, paramContext, this, isReminding)
      }.getOrElse {
        DBIO.successful(SimpleTextResult(event, Some(conversation), "All done!", conversation.behaviorVersion.responseType))
      }
    } yield result
  }

}

object ParamCollectionState {

  def fromAction(
                  conversation: Conversation,
                  event: Event,
                  services: DefaultServices
                )(implicit ec: ExecutionContext): DBIO[ParamCollectionState] = {
    val dataService = services.dataService
    for {
      params <- dataService.behaviorParameters.allForAction(conversation.behaviorVersion)
      collected <- dataService.collectedParameterValues.allForAction(conversation)
      user <- event.ensureUserAction(dataService)
      savedAnswers <- dataService.savedAnswers.allForAction(user, params)
    } yield ParamCollectionState(params, collected, savedAnswers, event, services)
  }

  def from(
            conversation: Conversation,
            event: Event,
            services: DefaultServices
          )(implicit ec: ExecutionContext): Future[ParamCollectionState] = {
    services.dataService.run(fromAction(conversation, event, services))
  }

}
