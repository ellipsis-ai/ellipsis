package controllers

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import models.ViewConfig
import models.accounts.user.UserTeamAccess
import models.silhouette.EllipsisEnv
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, InjectedController}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

trait EllipsisController extends InjectedController with I18nSupport {

  val assetsProvider: Provider[RemoteAssets]
  def assets: RemoteAssets = assetsProvider.get

  def viewConfig(maybeTeamAccess: Option[UserTeamAccess]) = ViewConfig(assets, maybeTeamAccess)

  def maybeTeamAccessFor(request: UserAwareRequest[EllipsisEnv, AnyContent], dataService: DataService)
                        (implicit ec: ExecutionContext): Future[Option[UserTeamAccess]] = {
    request.identity.map { user =>
      dataService.users.teamAccessFor(user, None).map(Some(_))
    }.getOrElse(Future.successful(None))
  }

}
