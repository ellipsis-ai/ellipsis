package controllers

import javax.inject.Inject

import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global

class HealthController @Inject() (
                                    val messagesApi: MessagesApi,
                                    val configuration: Configuration,
                                    val dataService: DataService
                                  ) extends EllipsisController {

  def check = Action.async { implicit request =>
    // test that the database can be accessed
    dataService.teams.allTeams.map { teams =>
      configuration.getString("application.version").map { version =>
        Ok(s"Version: $version")
      }.getOrElse {
        NotFound("Couldn't find version")
      }
    }
  }

}
