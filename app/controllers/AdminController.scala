package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import models.bots._
import models.silhouette.EllipsisEnv
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Result}
import services.{AWSLambdaService, DataService}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AdminController @Inject() (
                                  val messagesApi: MessagesApi,
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val dataService: DataService,
                                  val lambdaService: AWSLambdaService
                                ) extends ReAuthable {

  private def withIsAdminCheck(
                                fn: () => Future[Result]
                              )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]) = {
    dataService.users.isAdmin(request.identity).flatMap { isAdmin =>
      if (isAdmin) {
        fn()
      } else {
        Future.successful(NotFound(views.html.notFound(None, None, None)))
      }
    }
  }

  def lambdaFunctions() = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      for {
        allFunctionNames <- lambdaService.listFunctionNames
        currentVersionIdsWithFunction <- dataService.behaviorVersions.currentIdsWithFunction
      } yield {
        val missing = currentVersionIdsWithFunction.diff(allFunctionNames)
        val current = currentVersionIdsWithFunction.intersect(allFunctionNames)
        val obsolete = allFunctionNames.diff(currentVersionIdsWithFunction)
        Ok(views.html.admin.listLambdaFunctions(missing, current, obsolete))
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

}
