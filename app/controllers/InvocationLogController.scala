package controllers

import java.time.{OffsetDateTime, ZoneOffset}
import javax.inject.Inject

import com.google.inject.Provider
import json.Formatting._
import json.{APIErrorData, APIErrorResultData, LogEntryData}
import models.behaviors.events.EventType
import models.behaviors.invocationtoken.InvocationToken
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc.Result
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class InvocationLogController @Inject() (
                                 val configuration: Configuration,
                                 val dataService: DataService,
                                 val assetsProvider: Provider[RemoteAssets],
                                 implicit val ec: ExecutionContext
                               ) extends EllipsisController {

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
      result <- maybeInvocationToken.map { invocationToken =>
        getLogsWithToken(behaviorIdOrNameOrTrigger, invocationToken, maybeFrom, maybeTo, maybeUserId, maybeOriginalEventType)
      }.getOrElse {
        val errorResult = APIErrorResultData(Seq(APIErrorData("Invalid or expired token", Some("token"))))
        Future.successful(BadRequest(Json.toJson(errorResult)))
      }
    } yield result
  }

  private def getLogsWithToken(
                                behaviorIdOrNameOrTrigger: String,
                                invocationToken: InvocationToken,
                                maybeFrom: Option[String],
                                maybeTo: Option[String],
                                maybeUserId: Option[String],
                                maybeOriginalEventType: Option[String]
                              ): Future[Result] = {
    for {
      maybeOriginatingBehavior <- dataService.behaviors.findWithoutAccessCheck(invocationToken.behaviorId)
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
          dataService.invocationLogEntries
            .allForBehavior(behavior, from, to, maybeUserId, maybeValidOriginalEventType)
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
        val errorMessage = InvocationLogController.noActionFoundMessage(behaviorIdOrNameOrTrigger)
        val errorResult = APIErrorResultData(Seq(APIErrorData(errorMessage, Some("behaviorId"))))
        NotFound(Json.toJson(errorResult))
      }
    }

  }
}

object InvocationLogController {
  def noActionFoundMessage(nameOrId: String): String = {
    s"""Couldn't find an action for `$nameOrId`.
       |
       |Either it’s invalid, or the action is not part of the same skill. Only logs for the current skill can be obtained.""".stripMargin
  }
}
