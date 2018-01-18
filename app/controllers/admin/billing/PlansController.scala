package controllers.admin.billing

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.RemoteAssets
import controllers.admin.AdminAuth
import models.billing.chargebee.ChargebeeService
import models.silhouette.EllipsisEnv
import play.api.Configuration
import services.DataService

import scala.concurrent.ExecutionContext


class PlansController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val dataService: DataService,
                                  val configuration: Configuration,
                                  val assetsProvider: Provider[RemoteAssets],
                                  val chargebee: ChargebeeService,
                                  implicit val ec: ExecutionContext
                                ) extends AdminAuth {


  def list() =  silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      chargebee.allPlans.map { plans =>
        Ok(views.html.admin.billing.plans.list(plans, viewConfig(None)))
      }
    })
  }
}
