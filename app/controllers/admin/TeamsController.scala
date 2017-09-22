package controllers.admin

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{AdminController, ReAuthable, RemoteAssets}
import models.silhouette.EllipsisEnv
import play.api.Configuration
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext

class TeamsController @Inject() (
                                  override val silhouette: Silhouette[EllipsisEnv],
                                  override val dataService: DataService,
                                  override val lambdaService: AWSLambdaService,
                                  override val configuration: Configuration,
                                  override val assetsProvider: Provider[RemoteAssets],
                                  override implicit val ec: ExecutionContext
                                ) extends AdminController(silhouette,dataService,lambdaService, configuration, assetsProvider, ec) {

  def index = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      dataService.teams.allTeams.map { teams =>
        Ok(views.html.admin.teams.index(teams))
      }
    })
  }

}


