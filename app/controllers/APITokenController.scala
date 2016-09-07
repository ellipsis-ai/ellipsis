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
        val action = for {
          token <- APITokenQueries.createFor(user, label)
        } yield Redirect(routes.APITokenController.listTokens(Some(token.id)))

        dataService.run(action)
      }
    )
  }

  def listTokens(maybeJustCreatedTokenId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- DBIO.from(dataService.users.teamAccessFor(user, None))
      tokens <- APITokenQueries.allFor(user)
    } yield Ok(views.html.api.listTokens(teamAccess, tokens.map(APITokenData.from), maybeJustCreatedTokenId))

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
          maybeToken <- APITokenQueries.find(id)
          _ <- maybeToken.map { token =>
            if (token.userId == user.id) {
              APITokenQueries.revoke(token)
            } else {
              DBIO.successful(Unit)
            }
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
