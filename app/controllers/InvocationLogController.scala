package controllers

import javax.inject.Inject

import org.joda.time.LocalDateTime
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.mvc.Action
import json.Formatting._
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InvocationLogController @Inject() (
                                 val messagesApi: MessagesApi,
                                 val configuration: Configuration,
                                 val dataService: DataService
                               ) extends EllipsisController {

  case class ParamValues(values: JsValue, timestamp: LocalDateTime)
  implicit val paramValuesWrites = Json.writes[ParamValues]

  def getParamValues(behaviorId: String, token: String) = Action.async { implicit request =>
    for {
      maybeTeam <- dataService.teams.findForToken(token)
      maybeBehaviorWithoutAccessCheck <- dataService.behaviors.findWithoutAccessCheck(behaviorId)
      maybeBehavior <- Future.successful(maybeBehaviorWithoutAccessCheck.filter { behavior =>
        maybeTeam.contains(behavior.team)
      })
      maybeLogEntries <- maybeBehavior.map { behavior =>
        dataService.invocationLogEntries.allForBehavior(behavior).map { entries =>
          Some(entries.filterNot(_.paramValues == JsNull))
        }
      }.getOrElse(Future.successful(None))
    } yield {
      maybeLogEntries.map { logEntries =>
        val paramValues: Seq[ParamValues] = logEntries.map { ea =>
          ParamValues(ea.paramValues, ea.createdAt)
        }
        Ok(Json.toJson(paramValues))
      }.getOrElse {
        NotFound("")
      }
    }
  }

}
