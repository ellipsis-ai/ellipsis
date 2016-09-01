package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models._
import models.accounts._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OAuth2ApiController @Inject() (
                                      val messagesApi: MessagesApi,
                                      val env: Environment[User, CookieAuthenticator],
                                      val models: Models,
                                      val socialProviderRegistry: SocialProviderRegistry)
  extends ReAuthable {

  def newApi(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = user.teamAccessFor(maybeTeamId).map { teamAccess =>
      teamAccess.maybeTargetTeam.map { _ =>
        Ok(views.html.oAuth2Api(teamAccess, None))
      }.getOrElse {
        NotFound("Team not accessible")
      }
    }

    models.run(action)
  }

  def edit(apiId: String, maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- user.teamAccessFor(maybeTeamId)
      maybeApi <- OAuth2ApiQueries.find(apiId)
    } yield {
      teamAccess.maybeTargetTeam.map { team =>
        Ok(views.html.oAuth2Api(teamAccess, maybeApi))
      }.getOrElse {
        NotFound("Team not accessible")
      }
    }

    models.run(action)
  }

  case class OAuth2ApiInfo(
                            maybeId: Option[String],
                            name: String,
                            authorizationUrl: String,
                            accessTokenUrl: String,
                            maybeNewApplicationUrl: Option[String],
                            maybeScopeDocumentationUrl: Option[String],
                            maybeTeamId: Option[String]
                          )


  private val saveOAuth2ApiForm = Form(
    mapping(
      "id" -> optional(nonEmptyText),
      "name" -> nonEmptyText,
      "authorizationUrl" -> nonEmptyText,
      "accessTokenUrl" -> nonEmptyText,
      "newApplicationUrl" -> optional(nonEmptyText),
      "scopeDocumentationUrl" -> optional(nonEmptyText),
      "teamId" -> optional(nonEmptyText)
    )(OAuth2ApiInfo.apply)(OAuth2ApiInfo.unapply)
  )

  def save = SecuredAction.async { implicit request =>
    val user = request.identity
    saveOAuth2ApiForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val action = for {
          maybeExistingApi <- info.maybeId.map { id =>
            OAuth2ApiQueries.find(id)
          }.getOrElse(DBIO.successful(None))
          api <- OAuth2ApiQueries.save(maybeExistingApi.map { existing =>
            existing.copy(
              name = info.name,
              authorizationUrl = info.authorizationUrl,
              accessTokenUrl = info.accessTokenUrl,
              maybeNewApplicationUrl = info.maybeNewApplicationUrl,
              maybeScopeDocumentationUrl = info.maybeScopeDocumentationUrl
            )
          }.getOrElse {
            OAuth2Api(
              IDs.next,
              info.name,
              info.authorizationUrl,
              info.accessTokenUrl,
              info.maybeNewApplicationUrl,
              info.maybeScopeDocumentationUrl,
              None
            )
          })
        } yield {
          Redirect(routes.OAuth2ApiController.edit(api.id, None))
        }

        models.run(action)
      }
    )
  }

}
