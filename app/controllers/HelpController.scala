package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.i18n.MessagesApi
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HelpController @Inject() (
                                   val messagesApi: MessagesApi,
                                   val configuration: Configuration,
                                   val dataService: DataService,
                                   val silhouette: Silhouette[EllipsisEnv]
                                 ) extends EllipsisController {

  def scheduledMessages = silhouette.UserAwareAction.async { implicit request =>
    request.identity.map { user =>
      dataService.users.teamAccessFor(user, None).map(Some(_))
    }.getOrElse(Future.successful(None)).map { maybeTeamAccess =>
      Ok(views.html.help.scheduledMessages("Scheduling actions", viewConfig(maybeTeamAccess)))
    }
  }

}
