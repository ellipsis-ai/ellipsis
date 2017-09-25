package controllers

import javax.inject.Inject

import com.google.inject.Provider
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import services.{AWSDynamoDBService, DataService}

import scala.concurrent.{ExecutionContext, Future}

class DefaultStorage @Inject() (
                                 val configuration: Configuration,
                                 val dataService: DataService,
                                 val dynamoDBService: AWSDynamoDBService,
                                 val assetsProvider: Provider[RemoteAssets],
                                 implicit val ec: ExecutionContext
                               ) extends EllipsisController {

  case class PutItemInfo(token: String, itemType: String, itemId: String, itemJson: String)

  val putItemForm = Form(
    mapping(
      "token" -> nonEmptyText,
      "itemType" -> nonEmptyText,
      "itemId" -> nonEmptyText,
      "item" -> nonEmptyText
    )(PutItemInfo.apply)(PutItemInfo.unapply)
  )

  def putItem = Action.async { implicit request =>
    putItemForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest("oops"))
      },
      info => {
        for {
          maybeTeam <- dataService.teams.findForInvocationToken(info.token)
          result <- maybeTeam.map { team =>
            dynamoDBService.putItem(info.itemId, Json.toJson(info.itemJson), info.itemType, team).map { _ =>
              Ok("success")
            }
          }.getOrElse(Future.successful(Forbidden("Invalid request token")))
        } yield result
      }
    )
  }

  def getItem(itemId: String, itemType: String, token: String) = Action.async { implicit request =>
    for {
      maybeTeam <- dataService.teams.findForInvocationToken(token)
      result <- maybeTeam.map { team =>
        dynamoDBService.getItem(itemId, itemType, team).map { maybeItem =>
          maybeItem.map { item =>
            Ok(item)
          }.getOrElse(NotFound("item not found"))
        }
      }.getOrElse(Future.successful(Forbidden("Invalid request token")))
    } yield result
  }

}
