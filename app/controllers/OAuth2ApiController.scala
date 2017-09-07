package controllers

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import models._
import models.accounts.oauth2api.{OAuth2Api, OAuth2GrantType}
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OAuth2ApiController @Inject() (
                                      val silhouette: Silhouette[EllipsisEnv],
                                      val dataService: DataService,
                                      val configuration: Configuration,
                                      val assetsProvider: Provider[RemoteAssets]
                                    ) extends ReAuthable {

  def list(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
    } yield {
      teamAccess.maybeTargetTeam.map { team =>
        Ok(
          views.html.oauth2api.list(
            viewConfig(Some(teamAccess)),
            apis
          )
        )
      }.getOrElse{
        NotFound("Team not accessible")
      }
    }
  }

  def newApi(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    dataService.users.teamAccessFor(user, maybeTeamId).map { teamAccess =>
      teamAccess.maybeTargetTeam.map { _ =>
        Ok(views.html.oauth2api.edit(viewConfig(Some(teamAccess)), None, OAuth2GrantType.values))
      }.getOrElse {
        NotFound("Team not accessible")
      }
    }
  }

  def edit(apiId: String, maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      maybeApi <- dataService.oauth2Apis.find(apiId)
    } yield {
      teamAccess.maybeTargetTeam.map { team =>
        Ok(views.html.oauth2api.edit(viewConfig(Some(teamAccess)), maybeApi, OAuth2GrantType.values))
      }.getOrElse {
        NotFound("Team not accessible")
      }
    }
  }

  case class OAuth2ApiInfo(
                            maybeId: Option[String],
                            name: String,
                            grantType: String,
                            maybeAuthorizationUrl: Option[String],
                            accessTokenUrl: String,
                            maybeNewApplicationUrl: Option[String],
                            maybeScopeDocumentationUrl: Option[String],
                            maybeTeamId: Option[String]
                          )


  private val saveOAuth2ApiForm = Form(
    mapping(
      "id" -> optional(nonEmptyText),
      "name" -> nonEmptyText,
      "grantType" -> nonEmptyText,
      "authorizationUrl" -> optional(nonEmptyText),
      "accessTokenUrl" -> nonEmptyText,
      "newApplicationUrl" -> optional(nonEmptyText),
      "scopeDocumentationUrl" -> optional(nonEmptyText),
      "teamId" -> optional(nonEmptyText)
    )(OAuth2ApiInfo.apply)(OAuth2ApiInfo.unapply)
  )

  def save = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    saveOAuth2ApiForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeExistingApi <- info.maybeId.map { id =>
            dataService.oauth2Apis.find(id)
          }.getOrElse(Future.successful(None))
          api <- dataService.oauth2Apis.save(maybeExistingApi.map { existing =>
            existing.copy(
              name = info.name,
              grantType = OAuth2GrantType.definitelyFind(info.grantType),
              maybeAuthorizationUrl = info.maybeAuthorizationUrl,
              accessTokenUrl = info.accessTokenUrl,
              maybeNewApplicationUrl = info.maybeNewApplicationUrl,
              maybeScopeDocumentationUrl = info.maybeScopeDocumentationUrl
            )
          }.getOrElse {
            OAuth2Api(
              IDs.next,
              info.name,
              OAuth2GrantType.definitelyFind(info.grantType),
              info.maybeAuthorizationUrl,
              info.accessTokenUrl,
              info.maybeNewApplicationUrl,
              info.maybeScopeDocumentationUrl,
              None
            )
          })
        } yield {
          Redirect(routes.OAuth2ApiController.edit(api.id, None))
        }
      }
    )
  }

}
