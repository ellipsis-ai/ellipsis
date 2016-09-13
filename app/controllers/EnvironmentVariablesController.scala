package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import json._
import json.Formatting._
import models.silhouette.EllipsisEnv
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import services.DataService
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnvironmentVariablesController @Inject() (
                                                 val messagesApi: MessagesApi,
                                                 val silhouette: Silhouette[EllipsisEnv],
                                                 val dataService: DataService
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
            val action = (for {
              maybeTeam <- DBIO.from(dataService.teams.find(data.teamId, user))
              maybeEnvironmentVariables <- maybeTeam.map { team =>
                DBIO.sequence(data.variables.map { envVarData =>
                  DBIO.from(dataService.environmentVariables.ensureFor(envVarData.name, envVarData.value, team))
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

            dataService.run(action)
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
            dataService.environmentVariables.deleteFor(name, team)
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
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      maybeEnvironmentVariables <- teamAccess.maybeTargetTeam.map { team =>
        dataService.environmentVariables.allFor(team).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield {
      teamAccess.maybeTargetTeam.map { team =>
        val envVars = maybeEnvironmentVariables.map(envVars => envVars).getOrElse(Seq())
        val jsonData = Json.toJson(EnvironmentVariablesData(team.id, envVars.map(ea => EnvironmentVariableData.withoutValueFor(ea))))
        Ok(views.html.listEnvironmentVariables(teamAccess, jsonData.toString))
      }.getOrElse{
        NotFound("Team not accessible")
      }
    }
  }

}
