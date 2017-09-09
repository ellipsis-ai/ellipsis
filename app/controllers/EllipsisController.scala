package controllers

import com.google.inject.Provider
import models.ViewConfig
import models.accounts.user.UserTeamAccess
import play.api.i18n.I18nSupport
import play.api.mvc.InjectedController

trait EllipsisController extends InjectedController with I18nSupport {

  val assetsProvider: Provider[RemoteAssets]
  def assets: RemoteAssets = assetsProvider.get

  def viewConfig(maybeTeamAccess: Option[UserTeamAccess]) = ViewConfig(assets, maybeTeamAccess)

}
