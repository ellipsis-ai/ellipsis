package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import org.joda.time.LocalDateTime
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.mvc.Action
import json.Formatting._
import models.behaviors.invocationlogentry.InvocationLogEntry
import org.joda.time.format.DateTimeFormat
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InvocationLogController @Inject() (
                                 val messagesApi: MessagesApi,
                                 val configuration: Configuration,
                                 val dataService: DataService
                               ) extends EllipsisController {

  case class LogEntryData(
                           paramValues: JsValue,
                           context: String,
                           userIdForContext: Option[String],
                           ellipsisUserId: Option[String],
                           timestamp: LocalDateTime
                         )

  object LogEntryData {
    def forEntry(entry: InvocationLogEntry, dataService: DataService): Future[LogEntryData] = {
      val eventualMaybeEllipsisUser = entry.maybeUserIdForContext.map { userIdForContext =>
        dataService.linkedAccounts.find(LoginInfo(entry.context, userIdForContext), entry.behaviorVersion.team.id).map { maybeAcc =>
          maybeAcc.map(_.user)
        }
      }.getOrElse(Future.successful(None))
      eventualMaybeEllipsisUser.map { maybeUser =>
        LogEntryData(
          entry.paramValues,
          entry.context,
          entry.maybeUserIdForContext,
          maybeUser.map(_.id),
          entry.createdAt
        )
      }
    }
  }
  implicit val logEntryWrites = Json.writes[LogEntryData]

  private val EARLIEST = LocalDateTime.parse("2016-01-01")
  private val LATEST = LocalDateTime.now
  private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  private def maybeTimestampFor(maybeString: Option[String]): Option[LocalDateTime] = {
    try {
      maybeString.map { str =>
        LocalDateTime.parse(str, formatter)
      }
    } catch {
      case e: IllegalArgumentException => None
    }
  }

  def getLogs(
               behaviorId: String,
               token: String,
               maybeFrom: Option[String],
               maybeTo: Option[String]
             ) = Action.async { implicit request =>
    for {
      maybeTeam <- dataService.teams.findForToken(token)
      maybeBehaviorWithoutAccessCheck <- dataService.behaviors.findWithoutAccessCheck(behaviorId)
      maybeBehavior <- Future.successful(maybeBehaviorWithoutAccessCheck.filter { behavior =>
        maybeTeam.contains(behavior.team)
      })
      maybeLogEntries <- maybeBehavior.map { behavior =>
        val from = maybeTimestampFor(maybeFrom).getOrElse(EARLIEST)
        val to = maybeTimestampFor(maybeTo).getOrElse(LATEST)
        dataService.invocationLogEntries.allForBehavior(behavior, from, to).map { entries =>
          Some(entries.filterNot(_.paramValues == JsNull))
        }
      }.getOrElse(Future.successful(None))
      maybeLogEntryData <- maybeLogEntries.map { logEntries =>
        Future.sequence(logEntries.map { ea =>
          LogEntryData.forEntry(ea, dataService)
        }).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield {
      maybeLogEntryData.map { logEntryData =>
        Ok(Json.toJson(logEntryData))
      }.getOrElse {
        NotFound("")
      }
    }
  }

}
