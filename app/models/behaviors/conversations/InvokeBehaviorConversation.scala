package models.behaviors.conversations

import models.IDs
import models.behaviors._
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageEvent
import models.behaviors.triggers.messagetrigger.MessageTrigger
import org.joda.time.DateTime
import play.api.cache.CacheApi
import services.{AWSLambdaConstants, AWSLambdaService, DataService}

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

  def updateStateTo(newState: String, dataService: DataService): Future[Conversation] = {
    dataService.conversations.save(this.copy(state = newState))
  }

  case class ParamInfo(params: Seq[BehaviorParameter], collected: Seq[CollectedParameterValue]) {

    val rankedParams = params.sortBy(_.rank)

    def maybeNextToCollect: Option[(BehaviorParameter, Option[CollectedParameterValue])] = {
      val tuples = rankedParams.map { ea => (ea, collected.find(_.parameter == ea)) }
      tuples.find { case(param, maybeCollected) => !maybeCollected.exists(_.isValid) }
    }

    def invocationMap: Map[String, String] = {
      rankedParams.zipWithIndex.map { case(ea, i) =>
        val maybeParamValue = collected.find(_.parameter.id == ea.id).map(_.valueString)
        (AWSLambdaConstants.invocationParamFor(i), maybeParamValue.getOrElse(""))
      }.toMap
    }
  }

  private def paramInfo(dataService: DataService): Future[ParamInfo] = {
    for {
      params <- dataService.behaviorParameters.allFor(behaviorVersion)
      collected <- dataService.collectedParameterValues.allFor(this)
    } yield ParamInfo(params, collected)
  }

  private def collectParamValueFrom(event: MessageEvent, info: ParamInfo, dataService: DataService, cache: CacheApi): Future[Conversation] = {
    for {
      updatedConversation <- info.maybeNextToCollect.map { case(param, maybeCollected) =>
        val potentialValue = event.context.relevantMessageText
        param.paramType.isValid(potentialValue, Some(this), param, cache).flatMap { isValid =>
          dataService.collectedParameterValues.ensureFor(param, this, potentialValue).map(_ => this)
        }
      }.getOrElse(Future.successful(this))
      updatedParamInfo <- paramInfo(dataService)
      updatedConversation <- if (updatedParamInfo.maybeNextToCollect.isDefined) {
        Future.successful(updatedConversation)
      } else {
        updatedConversation.updateStateTo(Conversation.DONE_STATE, dataService)
      }
    } yield updatedConversation
  }

  def updateWith(event: MessageEvent, lambdaService: AWSLambdaService, dataService: DataService, cache: CacheApi): Future[Conversation] = {
    import Conversation._
    import InvokeBehaviorConversation._

    paramInfo(dataService).flatMap { info =>
      state match {
        case NEW_STATE => updateStateTo(COLLECT_PARAM_VALUES_STATE, dataService)
        case COLLECT_PARAM_VALUES_STATE => collectParamValueFrom(event, info, dataService, cache)
        case DONE_STATE => Future.successful(this)
      }
    }

  }

  private def promptResultFor(info: ParamInfo, event: MessageEvent, dataService: DataService, cache: CacheApi): Future[BotResult] = {
    info.maybeNextToCollect.map { case(param, maybeCollected) =>
      param.prompt(this, maybeCollected, event, dataService, cache)
    }.getOrElse {
      Future.successful("All done!")
    }.map { prompt =>
      SimpleTextResult(prompt)
    }
  }

  def respond(event: MessageEvent, lambdaService: AWSLambdaService, dataService: DataService, cache: CacheApi): Future[BotResult] = {
    import Conversation._
    import InvokeBehaviorConversation._

    paramInfo(dataService).flatMap { info =>
      state match {
        case COLLECT_PARAM_VALUES_STATE => promptResultFor(info, event, dataService, cache)
        case DONE_STATE => {
          BehaviorResponse.buildFor(event, behaviorVersion, info.invocationMap, trigger, Some(this), lambdaService, dataService, cache).flatMap { br =>
            br.resultForFilledOut
          }
        }
      }
    }

  }

}

object InvokeBehaviorConversation {

  val COLLECT_PARAM_VALUES_STATE = "collect_param_values"

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
