package models.behaviors.builtins

import akka.actor.ActorSystem
import models.behaviors.BotResult
import models.behaviors.builtins.admin.{AdminLookupEllipsisUserBehavior, AdminLookupSlackUserBehavior, BuiltinAdminBehavior}
import models.behaviors.events.Event
import play.api.Logger
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

  def maybeFrom(event: Event, services: DefaultServices)(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BuiltinBehavior]] = {
    if (event.includesBotMention) {
      val relevantText = uneducateQuotes(event.relevantMessageText)
      val maybeNonAdminBuiltIn = maybeNonAdminBuiltinFrom(relevantText, event, services)
      if (maybeNonAdminBuiltIn.isDefined) {
        Future.successful(maybeNonAdminBuiltIn)
      } else {
        maybeAdminBuiltinFrom(relevantText, event, services)
      }
    } else {
      Future.successful(None)
    }
  }

  private def maybeNonAdminBuiltinFrom(relevantText: String, event: Event, services: DefaultServices): Option[BuiltinBehavior] = {
    relevantText match {
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
      case _ => None
    }
  }

  private def maybeAdminBuiltinFrom(relevantText: String, event: Event, services: DefaultServices)
                                   (implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[BuiltinBehavior]] = {
    relevantText match {
      case adminLookupUserRegex(userIdTypeOrNull, userId, teamIdTypeOrNull, teamIdOrNull) => {
        maybeAdminLookup(event, services, Option(userIdTypeOrNull), userId, Option(teamIdTypeOrNull), Option(teamIdOrNull))
      }
      case _ => Future.successful(None)
    }
  }

  private def maybeAdminLookup(
                                event: Event,
                                services: DefaultServices,
                                maybeUserIdType: Option[String],
                                userId: String,
                                maybeTeamIdType: Option[String],
                                maybeTeamId: Option[String]
                              )(
                                implicit actorSystem: ActorSystem,
                                ec: ExecutionContext
                              ): Future[Option[BuiltinAdminBehavior]] = {
    val dataService = services.dataService
    for {
      user <- event.ensureUser(dataService)
      isAdmin <- dataService.users.isAdmin(user)
      maybeTeam <- dataService.teams.find(user.teamId)
      maybeUserData <- maybeTeam.map { team =>
        dataService.users.userDataFor(user, team).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield {
      if (!isAdmin) {
        val teamInfo = maybeTeam.map { team => s"team ${team.name} (ID ${team.id})"}.getOrElse(s"team ID ${user.teamId} (NOT FOUND!)")
        Logger.warn(
          s"""User ID ${user.id} on ${teamInfo} attempted to trigger an admin action with a message in channel ${event.maybeChannel.getOrElse("(unknown)")} on ${event.eventContext.description}.
             |
             |User info: ${maybeUserData.map { userData => s"${userData.fullName.getOrElse("(unknown name)")} · ${userData.email.getOrElse("(unknown email)")}" }.getOrElse("(not available)")}
             |
             |Message text:
             |${event.messageText}
             |""".stripMargin)
        None
      } else {
        val maybeEllipsisTeamId = if (maybeTeamIdType.contains("ellipsis")) {
          maybeTeamId
        } else {
          None
        }
        val maybeSlackTeamId = if (maybeTeamIdType.contains("slack")) {
          maybeTeamId
        } else {
          None
        }

        if (maybeUserIdType.contains("slack")) {
          Some(AdminLookupSlackUserBehavior(userId, maybeEllipsisTeamId, maybeSlackTeamId, event, services))
        } else if (maybeUserIdType.isEmpty || maybeUserIdType.contains("ellipsis")) {
          Some(AdminLookupEllipsisUserBehavior(userId, event, services))
          // TODO: other types of IDs
          //    } else if (idType == "msteams") {
          //      Some(AdminLookupMsTeamsUserBehavior(userId, event, services))
        } else {
          None
        }
      }
    }
  }

}
