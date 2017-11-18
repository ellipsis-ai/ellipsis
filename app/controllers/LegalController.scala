package controllers

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import models.silhouette.EllipsisEnv
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class LegalController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val assetsProvider: Provider[RemoteAssets],
                                  val dataService: DataService,
                                  implicit val ec: ExecutionContext
                                ) extends EllipsisController {

  def privacyPolicy = silhouette.UserAwareAction.async { implicit request =>
    for {
      maybeTeamAccess <- maybeTeamAccessFor(request, dataService)
    } yield {
      Ok(views.html.legal.privacyPolicy(viewConfig(maybeTeamAccess)))
    }
  }

  def userAgreement = silhouette.UserAwareAction.async { implicit request =>
    for {
      maybeTeamAccess <- maybeTeamAccessFor(request, dataService)
    } yield {
      Ok(views.html.legal.userAgreement(viewConfig(maybeTeamAccess)))
    }
  }

}
