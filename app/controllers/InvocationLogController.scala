package controllers

import java.time.OffsetDateTime
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.mvc.Action
import models.behaviors.invocationlogentry.InvocationLogEntry
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
                           timestamp: OffsetDateTime
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

  private val EARLIEST = OffsetDateTime.parse("2016-01-01")
  private val LATEST = OffsetDateTime.now

  private def maybeTimestampFor(maybeString: Option[String]): Option[OffsetDateTime] = {
    try {
      maybeString.map { str =>
        OffsetDateTime.parse(str)
      }
    } catch {
      case e: IllegalArgumentException => None
    }
  }

  def getLogs(
               behaviorIdOrTrigger: String,
               token: String,
               maybeFrom: Option[String],
               maybeTo: Option[String]
             ) = Action.async { implicit request =>
    for {
      maybeInvocationToken <- dataService.invocationTokens.findNotExpired(token)
      maybeOriginatingBehavior <- maybeInvocationToken.map { invocationToken =>
        dataService.behaviors.findWithoutAccessCheck(invocationToken.behaviorId)
      }.getOrElse(Future.successful(None))
      maybeBehavior <- maybeOriginatingBehavior.flatMap { behavior =>
        behavior.maybeGroup.map { group =>
          dataService.behaviors.findByIdOrTrigger(behaviorIdOrTrigger, group)
        }
      }.getOrElse(Future.successful(None))
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
        NotFound(
          s"""Couldn't find action for `${behaviorIdOrTrigger}`
             |
             |Possible reasons:
             |- The token passed is invalid or expired
             |- The action is neither a valid action ID, nor does it match an action in the same skill you are calling from
           """.stripMargin)
      }
    }
  }

}
