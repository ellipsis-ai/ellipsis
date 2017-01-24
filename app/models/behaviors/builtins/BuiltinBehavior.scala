package models.behaviors.builtins

import models.behaviors.BotResult
import models.behaviors.events.MessageEvent
import services.{AWSLambdaService, DataService}

import scala.concurrent.Future

trait BuiltinBehavior {
  val event: MessageEvent
  val lambdaService: AWSLambdaService
  val dataService: DataService

  def result: Future[BotResult]
}

object BuiltinBehavior {

  def maybeFrom(event: MessageEvent, lambdaService: AWSLambdaService, dataService: DataService): Option[BuiltinBehavior] = {
    val setEnvironmentVariableRegex = s"""(?i)(?s)^set\\s+env\\s+(\\S+)\\s+(.*)$$""".r
    val unsetEnvironmentVariableRegex = s"""(?i)^unset\\s+env\\s+(\\S+)\\s*$$""".r
    val startLearnConversationRegex = s"""(?i)^learn\\s*$$""".r
    val unlearnRegex = s"""(?i)^unlearn\\s+(\\S+)""".r
    val helpRegex = s"""(?i)^help\\s*(\\S*.*)$$""".r
    val rememberRegex = s"""(?i)^(remember|\\^)\\s*$$""".r
    val scheduledRegex = s"""(?i)^scheduled$$""".r
    val scheduleRegex = s"""(?i)^schedule\\s+[`"“](.*?)[`"”](\\s+privately for everyone in this channel)?\\s+(.*)\\s*$$""".r
    val unscheduleRegex = s"""(?i)^unschedule\\s+[`"“](.*?)[`"”]\\s*$$""".r
    val resetBehaviorsRegex = """(?i)reset behaviors really really really""".r
    val setTimeZoneRegex = s"""(?i)^set default time\\s*zone to\\s(.*)$$""".r

    if (event.includesBotMention) {
      event.relevantMessageText match {
        case setEnvironmentVariableRegex(name, value) => Some(SetEnvironmentVariableBehavior(name, value, event, lambdaService, dataService))
        case unsetEnvironmentVariableRegex(name) => Some(UnsetEnvironmentVariableBehavior(name, event, lambdaService, dataService))
        case startLearnConversationRegex() => Some(LearnBehavior(event, lambdaService, dataService))
        case unlearnRegex(regexString) => Some(UnlearnBehavior(regexString, event, lambdaService, dataService))
        case helpRegex(helpString) => Some(DisplayHelpBehavior(helpString, event, lambdaService, dataService))
        case rememberRegex(cmd) => Some(RememberBehavior(event, lambdaService, dataService))
        case scheduledRegex() => Some(ListScheduledBehavior(event, lambdaService, dataService))
        case scheduleRegex(text, individually, recurrence) => Some(ScheduleBehavior(text, (individually != null), recurrence, event, lambdaService, dataService))
        case unscheduleRegex(text) => Some(UnscheduleBehavior(text, event, lambdaService, dataService))
        case resetBehaviorsRegex() => Some(ResetBehaviorsBehavior(event, lambdaService, dataService))
        case setTimeZoneRegex(tzString) => Some(SetDefaultTimeZoneBehavior(tzString, event, lambdaService, dataService))
        case _ => None
      }
    } else {
      None
    }
  }

}
