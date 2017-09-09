package controllers

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import json._
import json.Formatting._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.filters.csrf.CSRF
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class EnvironmentVariablesController @Inject() (
                                                 val silhouette: Silhouette[EllipsisEnv],
                                                 val dataService: DataService,
                                                 val configuration: Configuration,
                                                 val assetsProvider: Provider[RemoteAssets],
                                                 implicit val ec: ExecutionContext
                                               ) extends ReAuthable {

  case class EnvironmentVariablesInfo(teamId: String, dataJson: String)

  private val submitForm = Form(
    mapping(
      "teamId" -> nonEmptyText,
      "dataJson" -> nonEmptyText
    )(EnvironmentVariablesInfo.apply)(EnvironmentVariablesInfo.unapply)
  )

  def submit = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    submitForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[EnvironmentVariablesData] match {
          case JsSuccess(data, jsPath) => {
            for {
              maybeTeam <- dataService.teams.find(data.teamId, user)
              maybeEnvironmentVariables <- maybeTeam.map { team =>
                Future.sequence(data.variables.map { envVarData =>
                  dataService.teamEnvironmentVariables.ensureFor(envVarData.name, envVarData.value, team)
                }).map( vars => Some(vars.flatten) )
              }.getOrElse(Future.successful(None))
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
            }
          }
          case e: JsError => Future.successful(BadRequest("Malformatted data"))
        }
      }
    )
  }

  private val deleteForm = Form(
    "name" -> nonEmptyText
  )

  def delete = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    deleteForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      name => {
        for {
          maybeTeam <- dataService.teams.find(user.teamId)
          isDeleted <- maybeTeam.map { team =>
            dataService.teamEnvironmentVariables.deleteFor(name, team)
          }.getOrElse(Future.successful(false))
        } yield {
          if (isDeleted) {
            Ok("Deleted")
          } else {
            NotFound("Couldn't find env var to delete for this team")
          }
        }
      }
    )
  }

  def list(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          environmentVariables <- teamAccess.maybeTargetTeam.map { team =>
            dataService.teamEnvironmentVariables.allFor(team)
          }.getOrElse(Future.successful(Seq()))
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val config = EnvironmentVariablesListConfig(
              containerId = "environmentVariableList",
              csrfToken = CSRF.getToken(request).map(_.value),
              data = EnvironmentVariablesData(team.id, environmentVariables.map(ea => EnvironmentVariableData.withoutValueFor(ea)))
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "config/environmentvariables/list", Json.toJson(config)))
          }.getOrElse{
            NotFound("Team not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          result <- teamAccess.maybeTargetTeam.map { team =>
            val dataRoute = routes.EnvironmentVariablesController.list(maybeTeamId)
            Future.successful(Ok(views.html.environmentvariables.list(viewConfig(Some(teamAccess)), dataRoute)))
          }.getOrElse {
            reAuthFor(request, maybeTeamId)
          }

        } yield result
      }
    }
  }

}
