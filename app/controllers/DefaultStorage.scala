package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.{Team, Models}
import models.accounts.User
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Controller, Action}
import services.AWSDynamoDBService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DefaultStorage @Inject() (
                                val messagesApi: MessagesApi,
                                val env: Environment[User, CookieAuthenticator],
                                val configuration: Configuration,
                                val models: Models,
                                val dynamoDBService: AWSDynamoDBService,
                                socialProviderRegistry: SocialProviderRegistry)
  extends Controller {

  case class PutItemInfo(teamId: String, itemType: String, itemId: String, itemJson: String)

  val putItemForm = Form(
    mapping(
      "teamId" -> nonEmptyText, // TODO: replace me with a secure token
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
          maybeTeam <- Team.find(info.teamId)
        } yield {
            maybeTeam.map { team =>
              dynamoDBService.putItem(info.itemId, info.itemJson, info.itemType, team)
              Ok("success")
            }.getOrElse(NotFound(s"Team not found: ${info.teamId}"))
        }

        models.run(action)
      }
    )
  }

  def getItem(itemId: String, itemType: String, teamId: String) = Action.async { implicit request =>
    val action = for {
      maybeTeam <- Team.find(teamId)
    } yield {
        maybeTeam.map { team =>
          val result = dynamoDBService.getItem(itemId, itemType, team)
          Ok(result)
        }.getOrElse(NotFound(s"Team not found: ${teamId}"))
      }

    models.run(action)
  }

}
