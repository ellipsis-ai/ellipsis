package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import export.{BehaviorBackedDataTypeExporter, BehaviorBackedDataTypeZipImporter}
import models.silhouette.EllipsisEnv
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorBackedDataTypeController @Inject() (
                                                  val messagesApi: MessagesApi,
                                                  val silhouette: Silhouette[EllipsisEnv],
                                                  val dataService: DataService
                                                ) extends ReAuthable {

  def list(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      dataTypes <- teamAccess.maybeTargetTeam.map { team =>
        dataService.behaviorBackedDataTypes.allFor(team)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      teamAccess.maybeTargetTeam.map { team =>
        Ok(
          views.html.behaviorBackedDataTypeList(
            teamAccess,
            dataTypes
          )
        )
      }.getOrElse{
        NotFound("Team not accessible")
      }
    }
  }

  def export(id: String) = silhouette.SecuredAction.async { implicit request =>
    BehaviorBackedDataTypeExporter.maybeFor(id, request.identity, dataService).map { maybeExporter =>
      maybeExporter.map { exporter =>
        Ok.sendFile(exporter.getZipFile)
      }.getOrElse {
        NotFound(s"Behavior not found: $id")
      }
    }
  }

  def importZip(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    dataService.users.teamAccessFor(user, maybeTeamId).map { teamAccess =>
      teamAccess.maybeTargetTeam.map { team =>
        Ok(views.html.importBehaviorBackedDataTypeZip(teamAccess))
      }.getOrElse {
        NotFound(s"No accessible team")
      }}

  }

  case class ImportBehaviorZipInfo(teamId: String)

  private val importBehaviorBackedDataTypeZipForm = Form(
    mapping(
      "teamId" -> nonEmptyText
    )(ImportBehaviorZipInfo.apply)(ImportBehaviorZipInfo.unapply)
  )

  def doImportZip() = silhouette.SecuredAction.async { implicit request =>
    (for {
      formData <- request.body.asMultipartFormData
      zipFile <- formData.file("zipFile")
    } yield {
      importBehaviorBackedDataTypeZipForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(formWithErrors.errorsAsJson))
        },
        info => {
          for {
            maybeTeam <- dataService.teams.find(info.teamId, request.identity)
            maybeImporter <- Future.successful(maybeTeam.map { team =>
              BehaviorBackedDataTypeZipImporter(team, request.identity, zipFile.ref.file, dataService)
            })
            maybeDataType <- maybeImporter.map { importer =>
              importer.run.map(Some(_))
            }.getOrElse(Future.successful(None))
          } yield {
            maybeDataType.map { dataType =>
              Redirect(routes.BehaviorBackedDataTypeController.list(Some(info.teamId)))
            }.getOrElse {
              NotFound(s"Team not found: ${info.teamId}")
            }
          }
        }
      )
    }).getOrElse(Future.successful(BadRequest("")))

  }

  private val deleteForm = Form("id" -> nonEmptyText)

  def delete = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    deleteForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      dataTypeId => {
        for {
          maybeDataType <- dataService.behaviorBackedDataTypes.find(dataTypeId, user)
          _ <- maybeDataType.map { dataType =>
            dataService.behaviorBackedDataTypes.delete(dataType, user)
          }.getOrElse(Future.successful({}))
        } yield {
          Redirect(routes.BehaviorBackedDataTypeController.list(maybeDataType.map(_.team.id)))
        }
      }
    )
  }

}
