package controllers

import models.ViewConfig
import models.accounts.user.UserTeamAccess
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.Controller

trait EllipsisController extends Controller with I18nSupport {

  val configuration: Configuration

  def viewConfig(maybeTeamAccess: Option[UserTeamAccess]) = ViewConfig(configuration, maybeTeamAccess)

}
