package models.behaviors.builtins

import models.behaviors.BotResult
import models.behaviors.events.MessageContext
import services.{AWSLambdaService, DataService}

import scala.concurrent.Future

trait BuiltinBehavior {
  val messageContext: MessageContext
  val lambdaService: AWSLambdaService
  val dataService: DataService

  def result: Future[BotResult]
}

object BuiltinBehavior {

  def maybeFrom(messageContext: MessageContext, lambdaService: AWSLambdaService, dataService: DataService): Option[BuiltinBehavior] = {
    val setEnvironmentVariableRegex = s"""(?i)(?s)^set\\s+env\\s+(\\S+)\\s+(.*)$$""".r
    val unsetEnvironmentVariableRegex = s"""(?i)^unset\\s+env\\s+(\\S+)\\s*$$""".r
    val startLearnConversationRegex = s"""(?i)^learn\\s*$$""".r
    val unlearnRegex = s"""(?i)^unlearn\\s+(\\S+)""".r
    val helpRegex = s"""(?i)^help\\s*(\\S*.*)$$""".r
    val rememberRegex = s"""(?i)^(remember|\\^)\\s*$$""".r
    val scheduledRegex = s"""(?i)^scheduled$$""".r
    val scheduleRegex = s"""(?i)^schedule\\s+`(.*?)`\\s+(.*)\\s*$$""".r
    val unscheduleRegex = s"""(?i)^unschedule\\s+`(.*?)`\\s*$$""".r
    val resetBehaviorsRegex = """(?i)reset behaviors really really really""".r

    if (messageContext.includesBotMention) {
      messageContext.relevantMessageText match {
        case setEnvironmentVariableRegex(name, value) => Some(SetEnvironmentVariableBehavior(name, value, messageContext, lambdaService, dataService))
        case unsetEnvironmentVariableRegex(name) => Some(UnsetEnvironmentVariableBehavior(name, messageContext, lambdaService, dataService))
        case startLearnConversationRegex() => Some(LearnBehavior(messageContext, lambdaService, dataService))
        case unlearnRegex(regexString) => Some(UnlearnBehavior(regexString, messageContext, lambdaService, dataService))
        case helpRegex(helpString) => Some(DisplayHelpBehavior(helpString, messageContext, lambdaService, dataService))
        case rememberRegex(cmd) => Some(RememberBehavior(messageContext, lambdaService, dataService))
        case scheduledRegex() => Some(ListScheduledBehavior(messageContext, lambdaService, dataService))
        case scheduleRegex(text, recurrence) => Some(ScheduleBehavior(text, recurrence, messageContext, lambdaService, dataService))
        case unscheduleRegex(text) => Some(UnscheduleBehavior(text, messageContext, lambdaService, dataService))
        case resetBehaviorsRegex() => Some(ResetBehaviorsBehavior(messageContext, lambdaService, dataService))
        case _ => None
      }
    } else {
      None
    }
  }

}
