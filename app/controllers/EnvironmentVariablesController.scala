package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json._
import json.Formatting._
import models._
import models.accounts._
import models.accounts.user.User
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnvironmentVariablesController @Inject() (
                                                val messagesApi: MessagesApi,
                                                val env: Environment[User, CookieAuthenticator],
                                                val models: Models,
                                                val socialProviderRegistry: SocialProviderRegistry)
  extends ReAuthable {

  case class EnvironmentVariablesInfo(teamId: String, dataJson: String)

  private val submitForm = Form(
    mapping(
      "teamId" -> nonEmptyText,
      "dataJson" -> nonEmptyText
    )(EnvironmentVariablesInfo.apply)(EnvironmentVariablesInfo.unapply)
  )

  def submit = SecuredAction.async { implicit request =>
    val user = request.identity
    submitForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[EnvironmentVariablesData] match {
          case JsSuccess(data, jsPath) => {
            val action = (for {
              maybeTeam <- Team.find(data.teamId, user)
              maybeEnvironmentVariables <- maybeTeam.map { team =>
                DBIO.sequence(data.variables.map { envVarData =>
                  EnvironmentVariableQueries.ensureFor(envVarData.name, envVarData.value, team)
                }).map( vars => Some(vars.flatten) )
              }.getOrElse(DBIO.successful(None))
            } yield {
              maybeEnvironmentVariables.map { envVars =>
                Ok(
                  Json.toJson(
                    EnvironmentVariablesData(
                      data.teamId,
                      envVars.map( ea => EnvironmentVariableData.withoutValueFor(ea) )
                    )
                  )
                )
              }.getOrElse {
                NotFound(s"Team not found: ${data.teamId}")
              }
            }) transactionally

            models.run(action)
          }
          case e: JsError => Future.successful(BadRequest("Malformatted data"))
        }
      }
    )
  }

}
