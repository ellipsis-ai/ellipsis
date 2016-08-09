package models.bots.builtins

import models.bots.MessageContext
import services.AWSLambdaService
import slick.driver.PostgresDriver.api._

trait BuiltinBehavior {
  val messageContext: MessageContext
  val lambdaService: AWSLambdaService

  def run: DBIO[Unit]
}

object BuiltinBehavior {

  def maybeFrom(messageContext: MessageContext, lambdaService: AWSLambdaService): Option[BuiltinBehavior] = {
    val setEnvironmentVariableRegex = s"""(?i)(?s)^set\\s+env\\s+(\\S+)\\s+(.*)$$""".r
    val startLearnConversationRegex = s"""(?i)^learn\\s*$$""".r
    val unlearnRegex = s"""(?i)^unlearn\\s+(\\S+)""".r
    val helpRegex = s"""(?i)^help\\s*(\\S*.*)$$""".r
    val rememberRegex = s"""(?i)^(remember|\\^)\\s*$$""".r
    val scheduledRegex = s"""(?i)^scheduled$$""".r
    val scheduleRegex = s"""(?i)^schedule\\s+`(.*?)`\\s+(.*)\\s*$$""".r
    val resetBehaviorsRegex = """(?i)reset behaviors really really really""".r

    if (messageContext.includesBotMention) {
      messageContext.relevantMessageText match {
        case setEnvironmentVariableRegex(name, value) => Some(SetEnvironmentVariableBehavior(name, value, messageContext, lambdaService))
        case startLearnConversationRegex() => Some(LearnBehavior(messageContext, lambdaService))
        case unlearnRegex(regexString) => Some(UnlearnBehavior(regexString, messageContext, lambdaService))
        case helpRegex(helpString) => Some(DisplayHelpBehavior(helpString, messageContext, lambdaService))
        case rememberRegex(cmd) => Some(RememberBehavior(messageContext, lambdaService))
        case scheduledRegex() => Some(ListScheduledBehavior(messageContext, lambdaService))
        case scheduleRegex(text, recurrence) => Some(ScheduleBehavior(text, recurrence, messageContext, lambdaService))
        case resetBehaviorsRegex() => Some(ResetBehaviorsBehavior(messageContext, lambdaService))
        case _ => None
      }
    } else {
      None
    }
  }

}
