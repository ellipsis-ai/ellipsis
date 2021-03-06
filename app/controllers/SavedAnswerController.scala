package controllers

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class SavedAnswerController @Inject() (
                                         val silhouette: Silhouette[EllipsisEnv],
                                         val configuration: Configuration,
                                         val dataService: DataService,
                                         val ws: WSClient,
                                         val assetsProvider: Provider[RemoteAssets],
                                         implicit val ec: ExecutionContext
                                       ) extends ReAuthable {

  private val resetForm = Form(
    "inputId" -> nonEmptyText
  )

  def resetForUser = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    resetForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      inputId => {
        for {
          numDeleted <- dataService.savedAnswers.deleteForUser(inputId, user)
        } yield {
          val result = Map("numDeleted" -> numDeleted)
          Ok(Json.toJson(result))
        }
      }
    )
  }

  def resetForTeam = silhouette.SecuredAction.async { implicit request =>
    resetForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      inputId => {
        for {
          // TODO: ensure the user is allowed to do this
          numDeleted <- dataService.savedAnswers.deleteAllFor(inputId)
        } yield {
          val result = Map("numDeleted" -> numDeleted)
          Ok(Json.toJson(result))
        }
      }
    )
  }
}
