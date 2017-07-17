package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Result}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AdminController @Inject() (
                                  val messagesApi: MessagesApi,
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val dataService: DataService,
                                  val lambdaService: AWSLambdaService,
                                  val configuration: Configuration
                                ) extends ReAuthable {

  private def withIsAdminCheck(
                                fn: () => Future[Result]
                              )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]) = {
    dataService.users.isAdmin(request.identity).flatMap { isAdmin =>
      if (isAdmin) {
        fn()
      } else {
        Future.successful(NotFound(views.html.error.notFound(viewConfig(None), None, None)))
      }
    }
  }

  def lambdaFunctions() = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      lambdaService.partionedBehaviorFunctionNames.map { partitioned =>
        Ok(views.html.admin.listLambdaFunctions(partitioned.missing, partitioned.current, partitioned.obsolete))
      }
    })
  }

  def redeploy(versionId: String) = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      for {
        maybeBehaviorVersion <- dataService.behaviorVersions.findWithoutAccessCheck(versionId)
        _ <- maybeBehaviorVersion.map { version =>
          dataService.behaviorVersions.redeploy(version)
        }.getOrElse(Future.successful(Unit))
      } yield Redirect(routes.AdminController.lambdaFunctions())
    })
  }

  def redeployAll = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      // do this in the background and respond immediately
      dataService.behaviorVersions.redeployAllCurrentVersions
      Future.successful(Redirect(routes.AdminController.lambdaFunctions()).flashing("success" -> "Redeploying in the background…"))
    })
  }
}
