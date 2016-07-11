package models.bots.conversations

import models.IDs
import models.bots._
import models.bots.triggers.MessageTrigger
import org.joda.time.DateTime
import services.{AWSLambdaConstants, AWSLambdaService}
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class InvokeBehaviorConversation(
                                      id: String,
                                      trigger: MessageTrigger,
                                      context: String, // Slack, etc
                                      userIdForContext: String, // id for Slack, etc user
                                      startedAt: DateTime,
                                      state: String = Conversation.NEW_STATE
                                      ) extends Conversation {

  val conversationType = Conversation.INVOKE_BEHAVIOR

  private def paramPromptFor(param: BehaviorParameter): String = {
    s"To collect a value for `${param.name}`, what question should @ellipsis ask the user?"
  }

  def updateStateTo(newState: String): DBIO[Conversation] = {
    this.copy(state = newState).save
  }

  case class ParamInfo(params: Seq[BehaviorParameter], collected: Seq[CollectedParameterValue]) {

    val rankedParams = params.sortBy(_.rank)

    def maybeNextToCollect: Option[BehaviorParameter] = {
      rankedParams.find(ea => !collected.map(_.parameter).contains(ea))
    }

    def invocationMap: Map[String, String] = {
      rankedParams.zipWithIndex.map { case(ea, i) =>
        val maybeParamValue = collected.find(_.parameter == ea).map(_.valueString)
        (AWSLambdaConstants.invocationParamFor(i), maybeParamValue.getOrElse(""))
      }.toMap
    }
  }

  private def paramInfo: DBIO[ParamInfo] = {
    for {
      params <- BehaviorParameterQueries.allFor(behaviorVersion)
      collected <- CollectedParameterValueQueries.allFor(this)
    } yield ParamInfo(params, collected)
  }

  private def collectParamValueFrom(event: MessageEvent, info: ParamInfo): DBIO[Conversation] = {
    for {
      _ <- info.maybeNextToCollect.map { param =>
        CollectedParameterValue(param, this, event.context.relevantMessageText).save
      }.getOrElse(DBIO.successful(Unit))
      updatedParamInfo <- paramInfo
      updatedConversation <- if (updatedParamInfo.maybeNextToCollect.isDefined) {
        DBIO.successful(this)
      } else {
        updateStateTo(Conversation.DONE_STATE)
      }
    } yield updatedConversation
  }

  def updateWith(event: MessageEvent, lambdaService: AWSLambdaService): DBIO[Conversation] = {
    import Conversation._
    import InvokeBehaviorConversation._

    paramInfo.flatMap { info =>
      state match {
        case NEW_STATE => updateStateTo(COLLECT_PARAM_VALUES_STATE)
        case COLLECT_PARAM_VALUES_STATE => collectParamValueFrom(event, info)
        case DONE_STATE => DBIO.successful(this)
      }
    }

  }

  private def sendPromptFor(event: MessageEvent, info: ParamInfo): Unit = {
    val prompt = (for {
      param <- info.maybeNextToCollect
      question <- param.maybeQuestion
    } yield question).getOrElse("All done!")

    event.context.sendMessage(prompt)
  }

  def respond(event: MessageEvent, lambdaService: AWSLambdaService): DBIO[Unit] = {
    import Conversation._
    import InvokeBehaviorConversation._

    paramInfo.flatMap { info =>
      state match {
        case COLLECT_PARAM_VALUES_STATE => DBIO.successful(sendPromptFor(event, info))
        case DONE_STATE => BehaviorResponse.buildFor(event, behaviorVersion, info.invocationMap, trigger).map(_.runCode(lambdaService))
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
                 activatedTrigger: MessageTrigger
                 ): DBIO[InvokeBehaviorConversation] = {
    val newInstance = InvokeBehaviorConversation(IDs.next, activatedTrigger, context, userIdForContext, DateTime.now, Conversation.NEW_STATE)
    newInstance.save.map(_ => newInstance)
  }
}
