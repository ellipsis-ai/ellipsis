package controllers

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import models.silhouette.EllipsisEnv
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

class FormController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val services: DefaultServices,
                                  val assetsProvider: Provider[RemoteAssets],
                                  implicit val ec: ExecutionContext
                                ) extends ReAuthable {

  val configuration = services.configuration
  val dataService = services.dataService
  val lambdaService = services.lambdaService
  val cacheService = services.cacheService
  val ws = services.ws

  def form(formId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        Future.successful(Ok(""))
      }
      case Accepts.Html() => {
        for {
          maybeForm <- dataService.forms.find(formId)
        } yield maybeForm.map { form =>
          Ok(form.id)
        }.getOrElse(NotFound(""))
      }
    }
  }
}
