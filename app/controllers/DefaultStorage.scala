package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.{Models, Team}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Action
import services.AWSDynamoDBService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DefaultStorage @Inject() (
                                 val messagesApi: MessagesApi,
                                 val configuration: Configuration,
                                 val models: Models,
                                 val dynamoDBService: AWSDynamoDBService,
                                 socialProviderRegistry: SocialProviderRegistry)
  extends EllipsisController {

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
        val action = for {
          maybeTeam <- Team.findForToken(info.token)
          result <- maybeTeam.map { team =>
            DBIO.from(dynamoDBService.putItem(info.itemId, Json.toJson(info.itemJson), info.itemType, team).map { _ =>
              Ok("success")
            })
          }.getOrElse(DBIO.successful(Unauthorized("Invalid request token")))
        } yield result

        models.run(action)
      }
    )
  }

  def getItem(itemId: String, itemType: String, token: String) = Action.async { implicit request =>
    val action = for {
      maybeTeam <- Team.findForToken(token)
      result <- maybeTeam.map { team =>
        DBIO.from(dynamoDBService.getItem(itemId, itemType, team).map { maybeItem =>
          maybeItem.map { item =>
            Ok(item)
          }.getOrElse(NotFound("item not found"))
        })
      }.getOrElse(DBIO.successful(Unauthorized("Invalid request token")))
    } yield result

    models.run(action)
  }

}
