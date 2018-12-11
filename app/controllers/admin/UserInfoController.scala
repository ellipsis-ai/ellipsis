package controllers.admin

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.RemoteAssets
import javax.inject.Inject
import models.behaviors.events.EventUserData
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class UserInfoResponse(user: Option[EventUserData], userNotFound: Boolean)

class UserInfoController @Inject() (
                                     val silhouette: Silhouette[EllipsisEnv],
                                     val dataService: DataService,
                                     val configuration: Configuration,
                                     val assetsProvider: Provider[RemoteAssets],
                                     implicit val ec: ExecutionContext
                                   ) extends AdminAuth {
  implicit val userInfoResponseWrites = Json.writes[UserInfoResponse]

  def userDataFor(userId: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      for {
        maybeUser <- dataService.users.find(userId)
        maybeTeam <- maybeUser.map { user =>
          dataService.teams.find(user.teamId)
        }.getOrElse(Future.successful(None))
        maybeUserData <- (for {
          user <- maybeUser
          team <- maybeTeam
        } yield {
          dataService.users.userDataFor(user, team).map(Some(_))
        }).getOrElse(Future.successful(None))
      } yield {
        Ok(Json.toJson(UserInfoResponse(
          maybeUser.map { user =>
            maybeUserData.getOrElse(EventUserData.withoutProfile(user.id))
          },
          maybeUser.isEmpty
        )))
      }
    })
  }
}
