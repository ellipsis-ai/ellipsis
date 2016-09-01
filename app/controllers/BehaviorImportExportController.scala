package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import export.{BehaviorVersionImporter, BehaviorVersionZipImporter, BehaviorVersionExporter}
import json._
import json.Formatting._
import models._
import models.accounts._
import models.bots._
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.libs.ws.WSClient
import services.AWSLambdaService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorImportExportController @Inject() (
                                                val messagesApi: MessagesApi,
                                                val env: Environment[User, CookieAuthenticator],
                                                val configuration: Configuration,
                                                val models: Models,
                                                val lambdaService: AWSLambdaService,
                                                val testReportBuilder: BehaviorTestReportBuilder,
                                                val ws: WSClient,
                                                val cache: CacheApi,
                                                val socialProviderRegistry: SocialProviderRegistry)
  extends ReAuthable {

  def export(id: String) = SecuredAction.async { implicit request =>
    val action = BehaviorVersionExporter.maybeFor(id, request.identity).map { maybeExporter =>
      maybeExporter.map { exporter =>
        Ok.sendFile(exporter.getZipFile)
      }.getOrElse {
        NotFound(s"Behavior not found: $id")
      }
    }

    models.run(action)
  }

  def importZip(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = user.teamAccessFor(maybeTeamId).map { teamAccess =>
      teamAccess.maybeTargetTeam.map { team =>
        Ok(views.html.importBehaviorZip(teamAccess))
      }.getOrElse {
        NotFound(s"No accessible team")
      }
    }

    models.run(action)
  }

  case class ImportBehaviorZipInfo(teamId: String)

  private val importBehaviorZipForm = Form(
    mapping(
      "teamId" -> nonEmptyText
    )(ImportBehaviorZipInfo.apply)(ImportBehaviorZipInfo.unapply)
  )

  def doImportZip = SecuredAction.async { implicit request =>
    (for {
      formData <- request.body.asMultipartFormData
      zipFile <- formData.file("zipFile")
    } yield {
      importBehaviorZipForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(formWithErrors.errorsAsJson))
        },
        info => {
          val action = for {
            maybeTeam <- Team.find(info.teamId, request.identity)
            maybeImporter <- DBIO.successful(maybeTeam.map { team =>
              BehaviorVersionZipImporter(team, request.identity, lambdaService, zipFile.ref.file)
            })
            maybeBehaviorVersion <- maybeImporter.map { importer =>
              importer.run.map(Some(_))
            }.getOrElse(DBIO.successful(None))
          } yield {
            maybeBehaviorVersion.map { behaviorVersion =>
              Redirect(routes.BehaviorEditorController.edit(behaviorVersion.behavior.id))
            }.getOrElse {
              NotFound(s"Team not found: ${info.teamId}")
            }
          }

          models.run(action)
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

  def doImport = SecuredAction.async { implicit request =>
    val user = request.identity
    importBehaviorForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[BehaviorVersionData] match {
          case JsSuccess(data, jsPath) => {
            val action = for {
              maybeTeam <- Team.find(data.teamId, user)
              maybeImporter <- DBIO.successful(maybeTeam.map { team =>
                BehaviorVersionImporter(team, user, lambdaService, data)
              })
              maybeBehaviorVersion <- maybeImporter.map { importer =>
                importer.run.map(Some(_))
              }.getOrElse(DBIO.successful(None))
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

            models.run(action)
          }
          case e: JsError => Future.successful(BadRequest("Malformatted data"))
        }
      }
    )
  }

}
