package models.bots.conversations

import models.IDs
import models.bots._
import models.bots.behaviorparameter.BehaviorParameter
import models.bots.behaviorversion.BehaviorVersion
import models.bots.conversations.collectedparametervalue.CollectedParameterValue
import models.bots.conversations.conversation.Conversation
import models.bots.events.MessageEvent
import models.bots.triggers.messagetrigger.MessageTrigger
import org.joda.time.DateTime
import services.{AWSLambdaConstants, AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class InvalidParamValue(param: BehaviorParameter, value: String) {

  def prompt: String = param.paramType.invalidPromptFor(param, value)

}

case class InvokeBehaviorConversation(
                                      id: String,
                                      trigger: MessageTrigger,
                                      context: String, // Slack, etc
                                      userIdForContext: String, // id for Slack, etc user
                                      startedAt: DateTime,
                                      state: String = Conversation.NEW_STATE,
                                      maybeInvalidValue: Option[InvalidParamValue] = None
                                      ) extends Conversation {

  val conversationType = Conversation.INVOKE_BEHAVIOR

  def updateStateTo(newState: String, dataService: DataService): Future[Conversation] = {
    dataService.conversations.save(this.copy(state = newState))
  }

  case class ParamInfo(params: Seq[BehaviorParameter], collected: Seq[CollectedParameterValue]) {

    val rankedParams = params.sortBy(_.rank)

    def maybeNextToCollect: Option[BehaviorParameter] = {
      rankedParams.find(ea => !collected.map(_.parameter).exists(_.id == ea.id))
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

  private def collectParamValueFrom(event: MessageEvent, info: ParamInfo, dataService: DataService): Future[Conversation] = {
    for {
      updatedConversation <- info.maybeNextToCollect.map { param =>
        val potentialValue = event.context.relevantMessageText
        if (param.paramType.isValid(potentialValue)) {
          val collected = CollectedParameterValue(param, this, event.context.relevantMessageText)
          dataService.collectedParameterValues.save(collected).map(_ => this)
        } else {
          Future.successful(this.copy(maybeInvalidValue = Some(InvalidParamValue(param, potentialValue))))
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

  def updateWith(event: MessageEvent, lambdaService: AWSLambdaService, dataService: DataService): Future[Conversation] = {
    import Conversation._
    import InvokeBehaviorConversation._

    paramInfo(dataService).flatMap { info =>
      state match {
        case NEW_STATE => updateStateTo(COLLECT_PARAM_VALUES_STATE, dataService)
        case COLLECT_PARAM_VALUES_STATE => collectParamValueFrom(event, info, dataService)
        case DONE_STATE => Future.successful(this)
      }
    }

  }

  private def promptResultFor(event: MessageEvent, info: ParamInfo): BehaviorResult = {
    val prompt = maybeInvalidValue.map { invalid =>
      invalid.prompt
    }.getOrElse {
      (for {
        param <- info.maybeNextToCollect
        question <- param.maybeQuestion
      } yield question).getOrElse("All done!")
    }

    SimpleTextResult(prompt)
  }

  def respond(event: MessageEvent, lambdaService: AWSLambdaService, dataService: DataService): Future[BehaviorResult] = {
    import Conversation._
    import InvokeBehaviorConversation._

    paramInfo(dataService).flatMap { info =>
      state match {
        case COLLECT_PARAM_VALUES_STATE => Future.successful(promptResultFor(event, info))
        case DONE_STATE => {
          BehaviorResponse.buildFor(event, behaviorVersion, info.invocationMap, trigger, dataService).flatMap { br =>
            br.resultForFilledOut(lambdaService, dataService)
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
    val newInstance = InvokeBehaviorConversation(IDs.next, activatedTrigger, context, userIdForContext, DateTime.now, Conversation.NEW_STATE)
    dataService.conversations.save(newInstance).map(_ => newInstance)
  }
}
