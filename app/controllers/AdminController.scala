package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models._
import models.bots._
import models.silhouette.EllipsisEnv
import play.api.i18n.MessagesApi
import services.AWSLambdaService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

class AdminController @Inject() (
                                  val messagesApi: MessagesApi,
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val models: Models,
                                  val lambdaService: AWSLambdaService
                                ) extends ReAuthable {

  def lambdaFunctions() = silhouette.SecuredAction.async { implicit request =>
    val action = for {
      allFunctionNames <- DBIO.from(lambdaService.listFunctionNames)
      currentVersionIdsWithFunction <- BehaviorVersionQueries.currentIdsWithFunction
    } yield {
        val missing = currentVersionIdsWithFunction.diff(allFunctionNames)
        val current = currentVersionIdsWithFunction.intersect(allFunctionNames)
        val obsolete = allFunctionNames.diff(currentVersionIdsWithFunction)
        Ok(views.html.admin.listLambdaFunctions(missing, current, obsolete))
      }

    models.run(action)
  }

  def redeploy(versionId: String) = silhouette.SecuredAction.async { implicit request =>
    val action = for {
      maybeBehaviorVersion <- BehaviorVersionQueries.findWithoutAccessCheck(versionId)
      _ <- maybeBehaviorVersion.map { version =>
        version.redeploy(lambdaService)
      }.getOrElse(DBIO.successful(Unit))
    } yield Redirect(routes.AdminController.lambdaFunctions())

    models.run(action)
  }

}
