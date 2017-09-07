package controllers

import javax.inject.Inject

import com.google.inject.Provider
import play.api.Configuration
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global

class HealthController @Inject() (
                                    val configuration: Configuration,
                                    val dataService: DataService,
                                    val assetsProvider: Provider[RemoteAssets]
                                  ) extends EllipsisController {

  def check = Action.async { implicit request =>
    // test that the database can be accessed
    dataService.teams.allTeams.map { teams =>
      configuration.getOptional[String]("application.version").map { version =>
        Ok(s"Version: $version")
      }.getOrElse {
        NotFound("Couldn't find version")
      }
    }
  }

}
