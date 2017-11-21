package controllers.web.settings

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{ReAuthable, RemoteAssets}
import json._
import json.Formatting._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
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

  private val deleteForm = Form(
    "name" -> nonEmptyText
  )

  def list(maybeTeamId: Option[String], maybeNewVarsString: Option[String]) = silhouette.SecuredAction.async { implicit request =>
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
            val maybeNewVars = maybeNewVarsString.map { varsString =>
              varsString.
                split(",").
                map(_.trim).
                filterNot(ea => environmentVariables.exists(_.name == ea)).
                sorted.
                toSeq
            }
            val newVarsData = maybeNewVars.map { vars =>
              vars.map { ea =>
                EnvironmentVariableData(ea, isAlreadySavedWithValue = false, None)
              }
            }.getOrElse(Seq())
            val varsData = environmentVariables.map(ea => EnvironmentVariableData.withoutValueFor(ea))
            val config = EnvironmentVariablesListConfig(
              containerId = "environmentVariableList",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamAccess.isAdminAccess,
              data = EnvironmentVariablesData(team.id, varsData ++ newVarsData),
              focus = maybeNewVars.flatMap(_.headOption)
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "settings/environmentvariables/list", Json.toJson(config)))
          }.getOrElse{
            NotFound("Team not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield teamAccess.maybeTargetTeam.map { team =>
          val dataRoute = routes.EnvironmentVariablesController.list(maybeTeamId, maybeNewVarsString)
          Ok(views.html.web.settings.environment_variables.list(viewConfig(Some(teamAccess)), dataRoute))
        }.getOrElse {
          notFoundWithLoginFor(request, Some(teamAccess))
        }
      }
    }
  }

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



}
