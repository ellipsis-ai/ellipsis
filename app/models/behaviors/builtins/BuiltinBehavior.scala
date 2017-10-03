package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.{BotResult, ParameterWithValue}
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

trait BuiltinBehavior {
  val event: Event
  val services: DefaultServices

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult]
}

object BuiltinBehavior {

  def uneducateQuotes(text: String): String = {
    text.replaceAll("[“”]", "\"").replaceAll("[‘’]", "'")
  }

  def maybeFor(
                name: String,
                event: Event,
                parametersWithValues: Seq[ParameterWithValue],
                maybeConversation: Option[Conversation],
                services: DefaultServices
              ): Option[BuiltinBehavior] = {
    name match {
      case ListScheduledBehavior.`forAllId` => Some(ListScheduledBehavior(event, services, isForAllChannels = true))
      case ListScheduledBehavior.`forChannelId` => Some(ListScheduledBehavior(event, services, isForAllChannels = false))
      case ScheduleBehavior.builtinId => ScheduleBehavior.maybeFor(parametersWithValues, event, services)
      case _ => None
    }
  }

  val helpRegex: Regex = s"""(?i)^help\\s*(\\S*.*)$$""".r
  val resetBehaviorsRegex: Regex = """(?i)reset behaviors really really really""".r
  val setTimeZoneRegex: Regex = s"""(?i)^set default time\\s*zone to\\s(.*)$$""".r
  val revokeAuthRegex: Regex = s"""(?i)^revoke\\s+all\\s+tokens\\s+for\\s+(.*)""".r
  val feedbackRegex: Regex = s"""(?i)^(feedback|support): (.+)$$""".r

  def maybeFrom(event: Event, services: DefaultServices): Option[BuiltinBehavior] = {
    if (event.includesBotMention) {
      uneducateQuotes(event.relevantMessageText) match {
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
        case resetBehaviorsRegex() => Some(ResetBehaviorsBehavior(event, services))
        case setTimeZoneRegex(tzString) => Some(SetDefaultTimeZoneBehavior(tzString, event, services))
        case revokeAuthRegex(appName) => Some(RevokeAuthBehavior(appName, event, services))
        case feedbackRegex(feedbackType, message) => Some(FeedbackBehavior(feedbackType, message, event, services))
        case _ => None
      }
    } else {
      None
    }
  }

}
