package controllers.web.settings

import javax.inject.Inject
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.admin.AdminAuth
import controllers.{ReAuthable, RemoteAssets}
import json._
import json.Formatting._
import models.environmentvariable.TeamEnvironmentVariable
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
                                               ) extends ReAuthable with AdminAuth {

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
          isAdmin <- dataService.users.isAdmin(user)
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
              isAdmin,
              data = EnvironmentVariablesData(team.id, varsData ++ newVarsData, None),
              focus = maybeNewVars.flatMap(_.headOption)
            )
            Ok(views.js.shared.webpackLoader(
              viewConfig(Some(teamAccess)),
              "EnvironmentVariableListConfig",
              "environmentVariables",
              Json.toJson(config)
            ))
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
                      envVars.map(EnvironmentVariableData.withoutValueFor),
                      None
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

  def adminLoadValue(teamId: String, envVarName: String) = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck { () =>
      for {
        maybeTeam <- dataService.teams.find(teamId)
        maybeEnvVar <- maybeTeam.map { team =>
          dataService.teamEnvironmentVariables.find(envVarName, team)
        }.getOrElse(Future.successful(None))
      } yield {
        maybeTeam.map { team =>
          val envVars = Seq(maybeEnvVar.map { v =>
            EnvironmentVariableData(v.name, v.value.nonEmpty, Option(v.value).filter(_.nonEmpty))
          }).flatten
          val maybeError = if (envVars.isEmpty) {
            Some(s"Environment variable `${envVarName}` not found")
          } else {
            None
          }
          Ok(Json.toJson(EnvironmentVariablesData(team.id, envVars, maybeError)))
        }.getOrElse {
          NotFound("Team not found")
        }
      }
    }
  }



}
