package controllers.web.settings

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{ReAuthable, RemoteAssets}
import json._
import json.Formatting._
import models._
import models.behaviors.config.awsconfig.AWSConfig
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.filters.csrf.CSRF
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class AWSConfigController @Inject() (
                                      val silhouette: Silhouette[EllipsisEnv],
                                      val dataService: DataService,
                                      val configuration: Configuration,
                                      val assetsProvider: Provider[RemoteAssets],
                                      implicit val ec: ExecutionContext
                                    ) extends ReAuthable {

  val AWS_CONFIG_DOC_URL = "http://docs.aws.amazon.com/general/latest/gr/managing-aws-access-keys.html"

  def add(
                 maybeTeamId: Option[String],
                 maybeBehaviorGroupId: Option[String],
                 maybeBehaviorId: Option[String],
                 maybeRequiredAWSConfigNameInCode: Option[String]
               ) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          maybeRequiredAWSConfig <- maybeRequiredAWSConfigNameInCode.map { id =>
            dataService.requiredAWSConfigs.find(id)
          }.getOrElse(Future.successful(None))
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val newConfigId = IDs.next
            val config = AWSConfigEditConfig(
              containerId = "configEditor",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamAccess.isAdminAccess,
              teamId = team.id,
              configSaved = false,
              configId = newConfigId,
              name = maybeRequiredAWSConfig.map(_.nameInCode),
              requiredNameInCode = maybeRequiredAWSConfigNameInCode,
              accessKeyId = None,
              secretAccessKey = None,
              region = None,
              documentationUrl = AWS_CONFIG_DOC_URL,
              behaviorGroupId = maybeBehaviorGroupId,
              behaviorId = maybeBehaviorId
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "config/awsconfig/edit", Json.toJson(config)))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val dataRoute = routes.AWSConfigController.add(maybeTeamId, maybeBehaviorGroupId, maybeBehaviorId, maybeRequiredAWSConfigNameInCode)
            Ok(views.html.web.settings.awsconfig.edit(viewConfig(Some(teamAccess)), "Add an AWS configuration", dataRoute))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
    }
  }

  def edit(id: String, maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      isAdmin <- dataService.users.isAdmin(user)
      maybeConfig <- teamAccess.maybeTargetTeam.map { team =>
        dataService.awsConfigs.find(id).map { maybeConfig =>
          maybeConfig.filter(_.teamId == team.id)
        }
      }.getOrElse(Future.successful(None))
    } yield {
      render {
        case Accepts.JavaScript() => {
          (for {
            team <- teamAccess.maybeTargetTeam
            config <- maybeConfig
          } yield {
            val editConfig = AWSConfigEditConfig(
              containerId = "configEditor",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamAccess.isAdminAccess,
              teamId = team.id,
              configSaved = true,
              configId = config.id,
              name = Some(config.name),
              requiredNameInCode = None,
              accessKeyId = config.maybeAccessKey,
              secretAccessKey = config.maybeSecretKey,
              region = config.maybeRegion,
              documentationUrl = AWS_CONFIG_DOC_URL,
              behaviorGroupId = None,
              behaviorId = None
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "config/awsconfig/edit", Json.toJson(editConfig)))
          }).getOrElse {
            NotFound("Unknown AWS configuration")
          }
        }
        case Accepts.Html() => {
          (for {
            _ <- teamAccess.maybeTargetTeam
            _ <- maybeConfig
          } yield {
            val dataRoute = routes.AWSConfigController.edit(id, maybeTeamId)
            Ok(views.html.web.settings.awsconfig.edit(viewConfig(Some(teamAccess)), "Edit AWS configuration", dataRoute))
          }).getOrElse {
            NotFound(
              views.html.error.notFound(
                viewConfig(Some(teamAccess)),
                Some("AWS configuration not found"),
                Some("The AWS configuration you are trying to access could not be found."),
                Some(reAuthLinkFor(request, None))
              ))
          }
        }
      }
    }
  }

  case class AWSConfigInfo(
                            id: String,
                            name: String,
                            requiredNameInCode: Option[String],
                            accessKeyId: Option[String],
                            secretAccessKey: Option[String],
                            region: Option[String],
                            teamId: String,
                            maybeBehaviorGroupId: Option[String],
                            maybeBehaviorId: Option[String]
                          )

  private val saveForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "name" -> nonEmptyText,
      "requiredNameInCode" -> optional(nonEmptyText),
      "accessKeyId" -> optional(nonEmptyText),
      "secretAccessKey" -> optional(nonEmptyText),
      "region" -> optional(nonEmptyText),
      "teamId" -> nonEmptyText,
      "behaviorGroupId" -> optional(nonEmptyText),
      "behaviorId" -> optional(nonEmptyText)
    )(AWSConfigInfo.apply)(AWSConfigInfo.unapply)
  )

  def save = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    saveForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeTeam <- dataService.teams.find(info.teamId, user)
          maybeConfig <- (for {
            team <- maybeTeam
          } yield {
            val instance = AWSConfig(info.id, info.name, info.teamId, info.accessKeyId, info.secretAccessKey, info.region)
            dataService.awsConfigs.save(instance).map(Some(_))
          }).getOrElse(Future.successful(None))
          maybeBehaviorGroup <- info.maybeBehaviorGroupId.map { groupId =>
            dataService.behaviorGroups.find(groupId, user)
          }.getOrElse(Future.successful(None))
          maybeBehaviorGroupVersion <- maybeBehaviorGroup.map { group =>
            dataService.behaviorGroups.maybeCurrentVersionFor(group)
          }.getOrElse(Future.successful(None))
          _ <- (for {
            nameInCode <- info.requiredNameInCode
            groupVersion <- maybeBehaviorGroupVersion
          } yield {
            dataService.requiredAWSConfigs.findWithNameInCode(nameInCode, groupVersion).flatMap { maybeExisting =>
              maybeExisting.map { existing =>
                dataService.requiredAWSConfigs.save(existing.copy(maybeConfig = maybeConfig))
              }.getOrElse {
                val maybeConfigData = maybeConfig.map(AWSConfigData.from)
                dataService.run(
                  dataService.requiredAWSConfigs.createForAction(
                    RequiredAWSConfigData(None, nameInCode, maybeConfigData),
                    groupVersion
                  )
                )
              }
            }
          }).getOrElse(Future.successful({}))
        } yield {
          maybeConfig.map { config =>
            maybeBehaviorGroup.map { group =>
              Redirect(controllers.routes.BehaviorEditorController.edit(group.id, info.maybeBehaviorId))
            }.getOrElse {
              Redirect(routes.AWSConfigController.edit(config.id, Some(config.teamId)))
            }
          }.getOrElse {
            NotFound(s"Team not found: ${info.teamId}")
          }
        }
      }
    )
  }


}
