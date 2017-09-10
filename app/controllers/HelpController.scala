package controllers

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import models.silhouette.EllipsisEnv
import play.api.Configuration
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class HelpController @Inject() (
                                   val configuration: Configuration,
                                   val dataService: DataService,
                                   val silhouette: Silhouette[EllipsisEnv],
                                   val assetsProvider: Provider[RemoteAssets],
                                   implicit val ec: ExecutionContext
                                 ) extends EllipsisController {

  def scheduledMessages = silhouette.UserAwareAction.async { implicit request =>
    request.identity.map { user =>
      dataService.users.teamAccessFor(user, None).map(Some(_))
    }.getOrElse(Future.successful(None)).map { maybeTeamAccess =>
      Ok(views.html.help.scheduledMessages("Scheduling actions", viewConfig(maybeTeamAccess)))
    }
  }

}
