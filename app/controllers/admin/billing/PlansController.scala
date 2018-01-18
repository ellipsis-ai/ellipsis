package controllers.admin.billing

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.RemoteAssets
import models.silhouette.EllipsisEnv
import controllers.admin.AdminAuth
import play.api.Configuration
import services.DataService

import scala.concurrent.{ExecutionContext, Future}


class PlansController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val dataService: DataService,
                                  val configuration: Configuration,
                                  val assetsProvider: Provider[RemoteAssets],
                                  implicit val ec: ExecutionContext
                                ) extends AdminAuth {


  def list() =  silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      Future {
        Ok(views.html.admin.billing.plans.list(viewConfig(None)))
      }
    })
  }
}
