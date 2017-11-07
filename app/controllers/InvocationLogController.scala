package controllers

import java.time.{OffsetDateTime, ZoneOffset}
import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.LoginInfo
import models.behaviors.events.EventType
import models.behaviors.invocationlogentry.InvocationLogEntry
import play.api.Configuration
import play.api.libs.json.{JsNull, JsValue, Json}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class InvocationLogController @Inject() (
                                 val configuration: Configuration,
                                 val dataService: DataService,
                                 val assetsProvider: Provider[RemoteAssets],
                                 implicit val ec: ExecutionContext
                               ) extends EllipsisController {

  case class LogEntryData(
                           paramValues: JsValue,
                           context: String,
                           userIdForContext: Option[String],
                           ellipsisUserId: Option[String],
                           timestamp: OffsetDateTime,
                           originalEventType: Option[String]
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
          entry.createdAt,
          entry.maybeOriginalEventType.map(_.toString)
        )
      }
    }
  }
  implicit val logEntryWrites = Json.writes[LogEntryData]

  private val EARLIEST = OffsetDateTime.of(2016, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
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
               behaviorIdOrNameOrTrigger: String,
               token: String,
               maybeFrom: Option[String],
               maybeTo: Option[String],
               maybeUserId: Option[String],
               maybeOriginalEventType: Option[String]
             ) = Action.async { implicit request =>
    for {
      maybeInvocationToken <- dataService.invocationTokens.findNotExpired(token)
      maybeOriginatingBehavior <- maybeInvocationToken.map { invocationToken =>
        dataService.behaviors.findWithoutAccessCheck(invocationToken.behaviorId)
      }.getOrElse(Future.successful(None))
      maybeBehavior <- maybeOriginatingBehavior.flatMap { behavior =>
        behavior.maybeGroup.map { group =>
          dataService.behaviors.findByIdOrNameOrTrigger(behaviorIdOrNameOrTrigger, group)
        }
      }.getOrElse(Future.successful(None))
      maybeLogEntries <- maybeBehavior.map { behavior =>
        val from = maybeTimestampFor(maybeFrom).getOrElse(EARLIEST)
        val to = maybeTimestampFor(maybeTo).getOrElse(LATEST)
        val maybeValidOriginalEventType = EventType.maybeFrom(maybeOriginalEventType)
        if (maybeOriginalEventType.isDefined && maybeValidOriginalEventType.isEmpty) {
          // Return an empty list if the original event type specified is invalid
          Future.successful(Some(Seq()))
        } else {
          dataService.invocationLogEntries.allForBehavior(behavior, from, to, maybeUserId, maybeValidOriginalEventType)
            .map { entries =>
              Some(entries.filterNot(_.paramValues == JsNull))
            }
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
          s"""Couldn't find action for `${behaviorIdOrNameOrTrigger}`
             |
             |Possible reasons:
             |- The token passed is invalid or expired
             |- The action is neither a valid action ID, nor does it match an action in the same skill you are calling from
           """.stripMargin)
      }
    }
  }

}
