package controllers.admin

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.RemoteAssets
import models.silhouette.EllipsisEnv
import play.api.Configuration
import services.{AWSLambdaService, DataService}

import scala.concurrent.{ExecutionContext, Future}

class LambdasController @Inject()(
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val dataService: DataService,
                                  val lambdaService: AWSLambdaService,
                                  val configuration: Configuration,
                                  val assetsProvider: Provider[RemoteAssets],
                                  implicit val ec: ExecutionContext
                                ) extends AdminAuth {

  def list() = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      lambdaService.partitionedBehaviorGroupFunctionNames.map { partitioned =>
        Ok(views.html.admin.lambdas.list(partitioned.missing, partitioned.current, partitioned.obsolete))
      }
    })
  }

  def redeploy(versionId: String) = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      for {
        maybeGroupVersion <- dataService.behaviorGroupVersions.findWithoutAccessCheck(versionId)
        _ <- maybeGroupVersion.map { version =>
          dataService.behaviorGroupVersions.redeploy(version)
        }.getOrElse(Future.successful(Unit))
      } yield Redirect(controllers.admin.routes.LambdasController.list())
    })
  }

  def redeployAll = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      // do this in the background and respond immediately
      dataService.behaviorGroupVersions.redeployAllCurrentVersions
      Future.successful(Redirect(controllers.admin.routes.LambdasController.list()).flashing("success" -> "Redeploying in the background…"))
    })
  }


}
