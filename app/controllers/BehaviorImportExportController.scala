package controllers

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import export._
import json._
import json.Formatting._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import services.{AWSLambdaService, CacheService, DataService}

import scala.concurrent.{ExecutionContext, Future}

class BehaviorImportExportController @Inject() (
                                                 val silhouette: Silhouette[EllipsisEnv],
                                                 val dataService: DataService,
                                                 val cacheService: CacheService,
                                                 val lambdaService: AWSLambdaService,
                                                 val configuration: Configuration,
                                                 val assetsProvider: Provider[RemoteAssets],
                                                 implicit val ec: ExecutionContext
                                               ) extends ReAuthable {

  def export(id: String) = silhouette.SecuredAction.async { implicit request =>
    BehaviorGroupExporter.maybeFor(id, request.identity, dataService, cacheService).map { maybeExporter =>
      maybeExporter.map { exporter =>
        Ok.sendFile(exporter.getZipFile)
      }.getOrElse {
        NotFound(s"Skill not found: $id")
      }
    }
  }

  def importZip(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    dataService.users.teamAccessFor(user, maybeTeamId).map { teamAccess =>
      teamAccess.maybeTargetTeam.map { team =>
        Ok(views.html.behaviorimportexport.importZip(viewConfig(Some(teamAccess))))
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
              BehaviorGroupZipImporter(team, request.identity, zipFile.ref.file, dataService, cacheService)
            })
            maybeBehaviorGroup <- maybeImporter.map { importer =>
              importer.run
            }.getOrElse(Future.successful(None))
            maybeBehavior <- maybeBehaviorGroup.map { group =>
              dataService.behaviors.allForGroup(group).map(_.headOption)
            }.getOrElse(Future.successful(None))
          } yield {
            maybeBehavior.map { behavior =>
              Redirect(routes.BehaviorEditorController.edit(behavior.group.id, Some(behavior.id)))
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
        json.validate[BehaviorGroupData] match {
          case JsSuccess(data, jsPath) => {
            for {
              teamAccess <- dataService.users.teamAccessFor(user, Some(info.teamId))
              maybeImporter <- Future.successful(teamAccess.maybeTargetTeam.map { team =>
                BehaviorGroupImporter(team, user, data, dataService)
              })
              maybeBehaviorGroup <- maybeImporter.map { importer =>
                importer.run
              }.getOrElse(Future.successful(None))
              maybeBehavior <- maybeBehaviorGroup.map { group =>
                dataService.behaviors.allForGroup(group).map(_.headOption)
              }.getOrElse(Future.successful(None))
              maybeBehaviorGroupData <- maybeBehaviorGroup.map { group =>
                BehaviorGroupData.maybeFor(group.id, user, None, dataService, cacheService)
              }.getOrElse(Future.successful(None))
            } yield {
              maybeBehaviorGroupData.map { groupData =>
                if (request.headers.get("x-requested-with").contains("XMLHttpRequest")) {
                  Ok(Json.toJson(groupData))
                } else {
                  maybeBehavior.map { behavior =>
                    Redirect(routes.BehaviorEditorController.edit(behavior.group.id, Some(behavior.id)))
                  }.getOrElse {
                    Redirect(routes.ApplicationController.index())
                  }
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
