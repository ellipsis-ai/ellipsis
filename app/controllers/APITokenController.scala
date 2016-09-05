package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import models._
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import slick.dbio.DBIO
import json.APITokenData
import models.silhouette.EllipsisEnv
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class APITokenController @Inject() (
                                     val messagesApi: MessagesApi,
                                     val silhouette: Silhouette[EllipsisEnv],
                                     val configuration: Configuration,
                                     val dataService: DataService
                                   ) extends ReAuthable {

  case class CreateAPITokenInfo(teamId: String, label: String)

  private val createAPITokenForm = Form(
    mapping(
      "teamId" -> nonEmptyText,
      "label" -> nonEmptyText
    )(CreateAPITokenInfo.apply)(CreateAPITokenInfo.unapply)
  )

  def createToken  = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    createAPITokenForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val action = for {
          teamAccess <- DBIO.from(dataService.users.teamAccessFor(user, Some(info.teamId)))
          maybeToken <- teamAccess.maybeTargetTeam.map { team =>
            APITokenQueries.createFor(team, info.label).map(Some(_))
          }.getOrElse(DBIO.successful(None))
        } yield maybeToken.map { token =>
            Redirect(routes.APITokenController.listTokens(Some(token.id)))
          }.getOrElse {
            NotFound("")
          }

        dataService.run(action)
      }
    )
  }

  def listTokens(maybeJustCreatedTokenId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- DBIO.from(dataService.users.teamAccessFor(user, None))
      tokens <- APITokenQueries.allFor(teamAccess.loggedInTeam)
    } yield {
        teamAccess.maybeTargetTeam.map { _ =>
          Ok(views.html.api.listTokens(teamAccess, tokens.map(APITokenData.from), maybeJustCreatedTokenId))
        }.getOrElse {
          NotFound("")
        }
      }

    dataService.run(action)
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
        val action = for {
          teamAccess <- DBIO.from(dataService.users.teamAccessFor(user, None))
          maybeToken <- APITokenQueries.find(id, teamAccess.loggedInTeam)
          _ <- maybeToken.map { token =>
            APITokenQueries.revoke(token, teamAccess.loggedInTeam)
          }.getOrElse(DBIO.successful(Unit))
        } yield maybeToken.map { token =>
            Redirect(routes.APITokenController.listTokens())
          }.getOrElse {
            NotFound("")
          }

        dataService.run(action)
      }
    )
  }

}
