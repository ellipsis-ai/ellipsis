package controllers

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.mvc.{AnyContent, Result}
import services.{AWSLambdaService, DataService}

import scala.concurrent.{ExecutionContext, Future}

class AdminController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val dataService: DataService,
                                  val lambdaService: AWSLambdaService,
                                  val configuration: Configuration,
                                  val assetsProvider: Provider[RemoteAssets],
                                  implicit val ec: ExecutionContext
                                ) extends AuthAsAdmin {

  def lambdaFunctions() = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      lambdaService.partionedBehaviorGroupFunctionNames.map { partitioned =>
        Ok(views.html.admin.listLambdaFunctions(partitioned.missing, partitioned.current, partitioned.obsolete))
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
      } yield Redirect(routes.AdminController.lambdaFunctions())
    })
  }

  def redeployAll = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      // do this in the background and respond immediately
      dataService.behaviorGroupVersions.redeployAllCurrentVersions
      Future.successful(Redirect(routes.AdminController.lambdaFunctions()).flashing("success" -> "Redeploying in the background…"))
    })
  }


}
