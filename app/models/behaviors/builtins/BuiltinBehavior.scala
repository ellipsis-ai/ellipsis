package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.BotResult
import models.behaviors.events.Event
import services.{AWSLambdaService, DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}

trait BuiltinBehavior {
  val event: Event
  val lambdaService: AWSLambdaService
  val dataService: DataService

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult]
}

object BuiltinBehavior {

  def uneducateQuotes(text: String): String = {
    text.replaceAll("[“”]", "\"").replaceAll("[‘’]", "'")
  }

  private val setEnvironmentVariableRegex = s"""(?i)(?s)^set\\s+env\\s+(\\S+)\\s+(.*)$$""".r
  private val unsetEnvironmentVariableRegex = s"""(?i)^unset\\s+env\\s+(\\S+)\\s*$$""".r
  private val startLearnConversationRegex = s"""(?i)^learn\\s*$$""".r
  private val unlearnRegex = s"""(?i)^unlearn\\s+(\\S+)""".r
  private val helpRegex = s"""(?i)^help\\s*(\\S*.*)$$""".r
  private val rememberRegex = s"""(?i)^(remember|\\^)\\s*$$""".r
  private val scheduledRegex = s"""(?i)^scheduled$$""".r
  private val allScheduledRegex = s"""(?i)^all scheduled$$""".r
  private val scheduleRegex = s"""(?i)^schedule\\s+([`"'])(.*?)\\1(\\s+privately for everyone in this channel)?\\s+(.*)\\s*$$""".r
  private val unscheduleRegex = s"""(?i)^unschedule\\s+([`"'])(.*?)\\1\\s*$$""".r
  private val resetBehaviorsRegex = """(?i)reset behaviors really really really""".r
  private val setTimeZoneRegex = s"""(?i)^set default time\\s*zone to\\s(.*)$$""".r
  private val revokeAuthRegex = s"""(?i)^revoke\\s+all\\s+tokens\\s+for\\s+(.*)""".r

  def maybeFrom(event: Event, services: DefaultServices): Option[BuiltinBehavior] = {
    val lambdaService = services.lambdaService
    val dataService = services.dataService
    val configuration = services.configuration
    if (event.includesBotMention) {
      uneducateQuotes(event.relevantMessageText) match {
        case setEnvironmentVariableRegex(name, value) => Some(SetEnvironmentVariableBehavior(name, value, event, lambdaService, dataService))
        case unsetEnvironmentVariableRegex(name) => Some(UnsetEnvironmentVariableBehavior(name, event, lambdaService, dataService))
        case startLearnConversationRegex() => Some(LearnBehavior(event, lambdaService, dataService))
        case unlearnRegex(regexString) => Some(UnlearnBehavior(regexString, event, lambdaService, dataService))
        case helpRegex(helpString) => Some(DisplayHelpBehavior(
          Some(helpString),
          None,
          Some(0),
          includeNameAndDescription = true,
          includeNonMatchingResults = false,
          isFirstTrigger = true,
          event,
          services
        ))
        case rememberRegex(cmd) => Some(RememberBehavior(event, lambdaService, dataService))
        case scheduledRegex() => Some(ListScheduledBehavior(event, event.maybeChannel, lambdaService, dataService, configuration))
        case allScheduledRegex() => Some(ListScheduledBehavior(event, None, lambdaService, dataService, configuration))
        case scheduleRegex(_, text, individually, recurrence) => Some(ScheduleBehavior(text, (individually != null), recurrence, event, lambdaService, dataService))
        case unscheduleRegex(_, text) => Some(UnscheduleBehavior(text, event, lambdaService, dataService, configuration))
        case resetBehaviorsRegex() => Some(ResetBehaviorsBehavior(event, lambdaService, dataService))
        case setTimeZoneRegex(tzString) => Some(SetDefaultTimeZoneBehavior(tzString, event, lambdaService, dataService))
        case revokeAuthRegex(appName) => Some(RevokeAuthBehavior(appName, event, lambdaService, dataService))
        case _ => None
      }
    } else {
      None
    }
  }

}
