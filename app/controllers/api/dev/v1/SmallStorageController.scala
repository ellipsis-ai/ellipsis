package controllers.api.dev.v1

import javax.inject.Inject

import controllers.EllipsisController
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Action
import services.{AWSDynamoDBService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class SmallStorageController @Inject() (
                                 val messagesApi: MessagesApi,
                                 val configuration: Configuration,
                                 val dataService: DataService,
                                 val dynamoDBService: AWSDynamoDBService
                               ) extends EllipsisController {

  // returns a bunch of info about the storage like: total number of items, space used, access patters, etc.
  def show(token: String) = Action.async { implicit request =>
    for {
      maybeTeam <- dataService.teams.findForInvocationToken(token)
      result <- maybeTeam.map { team =>
          Future.successful(Ok(Json.obj("status" ->"OK", "team" -> (team.name ))))
      }.getOrElse(Future.successful(Unauthorized("Invalid request token")))
    } yield result
  }

}

