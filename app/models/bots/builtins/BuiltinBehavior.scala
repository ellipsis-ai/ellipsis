package models.bots.builtins

import models.bots.BehaviorResult
import models.bots.events.MessageContext
import services.{AWSLambdaService, DataService}
import slick.driver.PostgresDriver.api._

trait BuiltinBehavior {
  val messageContext: MessageContext
  val lambdaService: AWSLambdaService

  def result: DBIO[BehaviorResult]
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
        case startLearnConversationRegex() => Some(LearnBehavior(messageContext, lambdaService))
        case unlearnRegex(regexString) => Some(UnlearnBehavior(regexString, messageContext, lambdaService))
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
