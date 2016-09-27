package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models.silhouette.EllipsisEnv
import play.api.i18n.MessagesApi
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorBackedDataTypeController @Inject() (
                                                  val messagesApi: MessagesApi,
                                                  val silhouette: Silhouette[EllipsisEnv],
                                                  val dataService: DataService
                                                ) extends ReAuthable {

  def list(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      dataTypes <- teamAccess.maybeTargetTeam.map { team =>
        dataService.behaviorBackedDataTypes.allFor(team)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      teamAccess.maybeTargetTeam.map { team =>
        Ok(
          views.html.behaviorBackedDataTypeList(
            teamAccess,
            dataTypes
          )
        )
      }.getOrElse{
        NotFound("Team not accessible")
      }
    }
  }

}
