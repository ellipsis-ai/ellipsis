package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import export.BehaviorBackedDataTypeExporter
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

  case class SaveInfo(id: String, name: String)

  private val saveForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "name" -> nonEmptyText
    )(SaveInfo.apply)(SaveInfo.unapply)
  )

  def save = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    saveForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeDataType <- dataService.behaviorBackedDataTypes.find(info.id, user)
          _ <- maybeDataType.map { dataType =>
            dataService.behaviorBackedDataTypes.updateName(dataType.id, info.name)
          }.getOrElse(Future.successful({}))
        } yield Redirect(routes.BehaviorBackedDataTypeController.list())
      }
    )
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

}
