package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.BotResult
import models.behaviors.builtins.admin.{LookupEllipsisUserBehavior, LookupSlackUserBehavior}
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

  val helpRegex: Regex = s"""(?i)^help\\s*(\\S*.*)$$""".r
  val scheduledRegex: Regex = s"""(?i)^scheduled$$""".r
  val allScheduledRegex: Regex = s"""(?i)^all scheduled$$""".r
  val scheduleRegex: Regex = s"""(?i)^schedule\\s+([`"'])(.*?)\\1(\\s+privately for everyone in this channel)?\\s+(.*)\\s*$$""".r
  val unscheduleRegex: Regex = s"""(?i)^unschedule\\s+([`"'])(.*?)\\1\\s*$$""".r
  val resetBehaviorsRegex: Regex = """(?i)reset behaviors really really really""".r
  val setTimeZoneRegex: Regex = s"""(?i)^set default time\\s*zone to\\s(.*)$$""".r
  val revokeAuthRegex: Regex = s"""(?i)^revoke\\s+all\\s+tokens\\s+for\\s+(.*)""".r
  val feedbackRegex: Regex = s"""(?i)^(feedback|support): (.+)$$""".r
  val helloRegex: Regex = s"""(?i)^hello|hi|ola|ciao|bonjour$$""".r
  val enableDevModeChannelRegex = s"""(?i)^enable dev mode$$""".r
  val disableDevModeChannelRegex = s"""(?i)^disable dev mode$$""".r
  val adminLookupUserRegex: Regex = s"""(?i)^admin (?:lookup|whois|who is)\\s*(slack|ellipsis|msteams)?\\s*user(?:\\s*id)? (\\S+)(?: on (slack|ellipsis|msteams) team(?:\\s*id)? (\\S+))?$$""".r

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
        case scheduledRegex() => Some(ListScheduledBehavior(event, event.maybeChannel, services))
        case allScheduledRegex() => Some(ListScheduledBehavior(event, None, services))
        case scheduleRegex(_, text, individually, recurrence) => Some(ScheduleBehavior(text, (individually != null), recurrence, event, services))
        case unscheduleRegex(_, text) => Some(UnscheduleBehavior(text, event, services))
        case resetBehaviorsRegex() => Some(ResetBehaviorsBehavior(event, services))
        case setTimeZoneRegex(tzString) => Some(SetDefaultTimeZoneBehavior(tzString, event, services))
        case revokeAuthRegex(appName) => Some(RevokeAuthBehavior(appName, event, services))
        case feedbackRegex(feedbackTrigger, message) => {
          val feedbackType = if (feedbackTrigger == "support") {
            "Chat support"
          } else {
            "Chat feedback"
          }
          Some(FeedbackBehavior(feedbackType, message, event, services))
        }
        case helloRegex() => Some(HelloBehavior(event, services))
        case enableDevModeChannelRegex() => Some(EnableDevModeChannelBehavior(event, services))
        case disableDevModeChannelRegex() => Some(DisableDevModeChannelBehavior(event, services))
        case adminLookupUserRegex(userIdTypeOrNull, userId, teamIdTypeOrNull, teamIdOrNull) => {
          val maybeTeamIdType = Option(teamIdTypeOrNull).map(_.toLowerCase)
          val maybeEllipsisTeamId = if (maybeTeamIdType.contains("ellipsis")) {
            Option(teamIdOrNull)
          } else {
            None
          }
          val maybeSlackTeamId = if (maybeTeamIdType.contains("slack")) {
            Option(teamIdOrNull)
          } else {
            None
          }
          val maybeUserIdType = Option(userIdTypeOrNull)
          if (maybeUserIdType.contains("slack")) {
            Some(LookupSlackUserBehavior(userId, maybeEllipsisTeamId, maybeSlackTeamId, event, services))
          } else if (maybeUserIdType.isEmpty || maybeUserIdType.contains("ellipsis")) {
            Some(LookupEllipsisUserBehavior(userId, maybeEllipsisTeamId, event, services))
// TODO: other types of IDs
//        } else if (idType == "msteams") {
//          Some(LookupMsTeamsUserBehavior(userId, event, services))
          } else {
            None
          }
        }
        case _ => None
      }
    } else {
      None
    }
  }

}
