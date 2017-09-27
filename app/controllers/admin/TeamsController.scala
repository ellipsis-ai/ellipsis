package controllers.admin

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{AdminController, AuthAsAdmin, ReAuthable, RemoteAssets}
import models.silhouette.EllipsisEnv
import play.api.Configuration
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext

class TeamsController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val dataService: DataService,
                                  val lambdaService: AWSLambdaService,
                                  val configuration: Configuration,
                                  val assetsProvider: Provider[RemoteAssets],
                                  implicit val ec: ExecutionContext
                                ) extends AuthAsAdmin {

  def list(currentPage: Int, perPage: Int) = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      dataService.teams.allTeamsPaged(currentPage, perPage).map { teams =>
        Ok(views.html.admin.teams.list(viewConfig(None), teams, currentPage, perPage))
      }
    })
  }

}


