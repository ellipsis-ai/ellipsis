package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import export.{BehaviorVersionExporter, BehaviorVersionImporter, BehaviorVersionZipImporter}
import json._
import json.Formatting._
import models._
import models.silhouette.EllipsisEnv
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import services.{AWSLambdaService, DataService}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorImportExportController @Inject() (
                                                 val messagesApi: MessagesApi,
                                                 val silhouette: Silhouette[EllipsisEnv],
                                                 val dataService: DataService,
                                                 val lambdaService: AWSLambdaService
                                               ) extends ReAuthable {

  def export(id: String) = silhouette.SecuredAction.async { implicit request =>
    BehaviorVersionExporter.maybeFor(id, request.identity, dataService).map { maybeExporter =>
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
        Ok(views.html.importBehaviorZip(teamAccess))
      }.getOrElse {
        NotFound(s"No accessible team")
      }}

  }

  case class ImportBehaviorZipInfo(teamId: String)

  private val importBehaviorZipForm = Form(
    mapping(
      "teamId" -> nonEmptyText
    )(ImportBehaviorZipInfo.apply)(ImportBehaviorZipInfo.unapply)
  )

  def doImportZip() = silhouette.SecuredAction.async { implicit request =>
    (for {
      formData <- request.body.asMultipartFormData
      zipFile <- formData.file("zipFile")
    } yield {
      importBehaviorZipForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(formWithErrors.errorsAsJson))
        },
        info => {
          for {
            maybeTeam <- dataService.teams.find(info.teamId, request.identity)
            maybeImporter <- Future.successful(maybeTeam.map { team =>
              BehaviorVersionZipImporter(team, request.identity, lambdaService, zipFile.ref.file, dataService)
            })
            maybeBehaviorVersion <- maybeImporter.map { importer =>
              importer.run.map(Some(_))
            }.getOrElse(Future.successful(None))
          } yield {
            maybeBehaviorVersion.map { behaviorVersion =>
              Redirect(routes.BehaviorEditorController.edit(behaviorVersion.behavior.id))
            }.getOrElse {
              NotFound(s"Team not found: ${info.teamId}")
            }
          }
        }
      )
    }).getOrElse(Future.successful(BadRequest("")))

  }

  case class ImportBehaviorInfo(teamId: String, dataJson: String)

  private val importBehaviorForm = Form(
    mapping(
      "teamId" -> nonEmptyText,
      "dataJson" -> nonEmptyText
    )(ImportBehaviorInfo.apply)(ImportBehaviorInfo.unapply)
  )

  def doImport() = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    importBehaviorForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[BehaviorVersionData] match {
          case JsSuccess(data, jsPath) => {
            for {
              maybeTeam <- dataService.teams.find(data.teamId, user)
              maybeImporter <- Future.successful(maybeTeam.map { team =>
                BehaviorVersionImporter(team, user, data, dataService)
              })
              maybeBehaviorVersion <- maybeImporter.map { importer =>
                importer.run.map(Some(_))
              }.getOrElse(Future.successful(None))
            } yield {
              maybeBehaviorVersion.map { behaviorVersion =>
                if (request.headers.get("x-requested-with").contains("XMLHttpRequest")) {
                  Ok(Json.obj("behaviorId" -> behaviorVersion.behavior.id))
                } else {
                  Redirect(routes.BehaviorEditorController.edit(behaviorVersion.behavior.id, justSaved = Some(true)))
                }
              }.getOrElse {
                NotFound("Behavior not found")
              }
            }
          }
          case e: JsError => Future.successful(BadRequest("Malformatted data"))
        }
      }
    )
  }

}
