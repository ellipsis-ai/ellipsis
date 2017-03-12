package controllers.api.dev.v1.small_storage

import javax.inject.Inject

import controllers.EllipsisController
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.Action
import services.{AWSDynamoDBService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by matteo on 11/29/16.
  */
class ItemsController @Inject() (
                                  val messagesApi: MessagesApi,
                                  val configuration: Configuration,
                                  val dataService: DataService,
                                  val dynamoDBService: AWSDynamoDBService
                                ) extends EllipsisController {


  def index(token: String) = Action.async { implicit request =>
    for {
      maybeTeam <- dataService.teams.findForToken(token)
      result <- maybeTeam.map { team =>
        val json = Json.obj(
          "object" -> "list",
          "url" -> "api/dev/v1/small_storage/items",
          "total_count" -> 0,
          "has_more" -> false,
          "data" -> JsArray()
        )
        Future.successful(Ok(json))
      }.getOrElse(Future.successful(Unauthorized("Invalid request token")))
    } yield result
  }


}
