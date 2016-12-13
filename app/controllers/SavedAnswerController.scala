package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.libs.ws.WSClient
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SavedAnswerController @Inject() (
                                         val messagesApi: MessagesApi,
                                         val silhouette: Silhouette[EllipsisEnv],
                                         val configuration: Configuration,
                                         val dataService: DataService,
                                         val cache: CacheApi,
                                         val ws: WSClient
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
          maybeInput <- dataService.inputs.find(inputId)
          maybeNumDeleted <- maybeInput.map { input =>
            dataService.savedAnswers.deleteForUser(input, user).map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield {
          maybeNumDeleted.map { numDeleted =>
            val result = Map("numDeleted" -> numDeleted)
            Ok(Json.toJson(result))
          }.getOrElse {
            NotFound("")
          }
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
          maybeInput <- dataService.inputs.find(inputId)
          maybeNumDeleted <- maybeInput.map { input =>
            dataService.savedAnswers.deleteAllFor(input).map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield {
          maybeNumDeleted.map { numDeleted =>
            val result = Map("numDeleted" -> numDeleted)
            Ok(Json.toJson(result))
          }.getOrElse {
            NotFound("")
          }
        }
      }
    )
  }
}
