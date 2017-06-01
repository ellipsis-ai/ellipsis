package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import json.Formatting._
import json.{APITokenData, APITokenListConfig}
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.filters.csrf.CSRF
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APITokenController @Inject() (
                                     val messagesApi: MessagesApi,
                                     val silhouette: Silhouette[EllipsisEnv],
                                     val configuration: Configuration,
                                     val dataService: DataService
                                   ) extends ReAuthable {

  private val createAPITokenForm = Form(
    "label" -> nonEmptyText
  )

  def createToken  = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    createAPITokenForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      label => {
        dataService.apiTokens.createFor(user, label).map { token =>
          Redirect(routes.APITokenController.listTokens(Some(token.id)))
        }
      }
    )
  }

  def listTokens(maybeJustCreatedTokenId: Option[String], maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          tokens <- if (teamAccess.isAdminAccess) {
            Future.successful(Seq())
          } else {
            dataService.apiTokens.allFor(user)
          }
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val config = APITokenListConfig(
              containerId = "apiTokenGenerator",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamId = team.id,
              tokens = tokens.map(APITokenData.from),
              justCreatedTokenId = maybeJustCreatedTokenId,
              canGenerateTokens = !teamAccess.isAdminAccess
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "config/api/listTokens", Json.toJson(config)))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { _ =>
            val dataRoute = routes.APITokenController.listTokens(maybeJustCreatedTokenId, maybeTeamId)
            Ok(views.html.api.listTokens(viewConfig(Some(teamAccess)), dataRoute))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
    }
  }

  private val revokeApiTokenForm = Form(
    "id" -> nonEmptyText
  )

  def revokeToken = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    revokeApiTokenForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      id => {
        for {
          maybeToken <- dataService.apiTokens.find(id)
          _ <- maybeToken.map { token =>
            if (token.userId == user.id) {
              dataService.apiTokens.revoke(token)
            } else {
              Future.successful(Unit)
            }
          }.getOrElse(Future.successful(Unit))
        } yield maybeToken.map { token =>
          Redirect(routes.APITokenController.listTokens())
        }.getOrElse {
          NotFound("")
        }
      }
    )
  }

}
