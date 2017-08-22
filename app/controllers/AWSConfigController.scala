package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import json._
import json.Formatting._
import models._
import models.behaviors.config.awsconfig.AWSConfig
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.filters.csrf.CSRF
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AWSConfigController @Inject() (
                                      val messagesApi: MessagesApi,
                                      val silhouette: Silhouette[EllipsisEnv],
                                      val dataService: DataService,
                                      val configuration: Configuration
                                    ) extends ReAuthable {

  val AWS_CONFIG_DOC_URL = "http://docs.aws.amazon.com/general/latest/gr/managing-aws-access-keys.html"

  def list(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          configs <- teamAccess.maybeTargetTeam.map { team =>
            dataService.awsConfigs.allFor(team)
          }.getOrElse(Future.successful(Seq()))
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val config = AWSConfigListConfig(
              containerId = "configList",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamId = team.id,
              configsData = configs.map(AWSConfigData.from)
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "config/awsconfig/list", Json.toJson(config)))
          }.getOrElse{
            NotFound("Team not found")
          }
        }
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val dataRoute = routes.AWSConfigController.list(maybeTeamId)
            Ok(views.html.awsconfig.list(viewConfig(Some(teamAccess)), dataRoute))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
    }
  }

  def newConfig(maybeTeamId: Option[String], maybeBehaviorId: Option[String], maybeRequiredAWSConfigId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val newConfigId = IDs.next
            val config = AWSConfigEditConfig(
              containerId = "configEditor",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamId = team.id,
              configSaved = false,
              configId = newConfigId,
              name = None,
              accessKeyId = None,
              secretAccessKey = None,
              region = None,
              documentationUrl = AWS_CONFIG_DOC_URL,
              behaviorId = maybeBehaviorId,
              requiredAWSConfigId = maybeRequiredAWSConfigId
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
            val dataRoute = routes.AWSConfigController.newConfig(maybeTeamId, maybeBehaviorId)
            Ok(views.html.awsconfig.edit(viewConfig(Some(teamAccess)), "Add an AWS configuration", dataRoute))
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
              teamId = team.id,
              configSaved = true,
              configId = config.id,
              name = Some(config.name),
              accessKeyId = config.maybeAccessKey,
              secretAccessKey = config.maybeSecretKey,
              region = config.maybeRegion,
              documentationUrl = AWS_CONFIG_DOC_URL,
              behaviorId = None,
              requiredAWSConfigId = None
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
            Ok(views.html.awsconfig.edit(viewConfig(Some(teamAccess)), "Edit AWS configuration", dataRoute))
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
                            accessKeyId: Option[String],
                            secretAccessKey: Option[String],
                            region: Option[String],
                            teamId: String,
                            maybeBehaviorId: Option[String]
                          )

  private val saveForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "name" -> nonEmptyText,
      "accessKeyId" -> optional(nonEmptyText),
      "secretAccessKey" -> optional(nonEmptyText),
      "region" -> optional(nonEmptyText),
      "teamId" -> nonEmptyText,
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
          maybeBehaviorVersion <- info.maybeBehaviorId.map { behaviorId =>
            dataService.behaviors.find(behaviorId, user).flatMap { maybeBehavior =>
              maybeBehavior.map { behavior =>
                dataService.behaviors.maybeCurrentVersionFor(behavior)
              }.getOrElse(Future.successful(None))
            }
          }.getOrElse(Future.successful(None))
          requiredAWSConfigs <- (for {
            behaviorVersion <- maybeBehaviorVersion
            group <- behaviorVersion.behavior.maybeGroup
          } yield {
            dataService.requiredAWSConfigs.allFor(group)
          }).getOrElse(Future.successful(Seq()))
          _ <- Future.sequence {
            requiredAWSConfigs.
              filter(_.maybeConfig.isEmpty).
              map { ea =>
                dataService.requiredAWSConfigs.save(ea.copy(maybeConfig = maybeConfig))
              }
          }
        } yield {
          maybeConfig.map { config =>
            maybeBehaviorVersion.map { behaviorVersion =>
              val behavior = behaviorVersion.behavior
              Redirect(routes.BehaviorEditorController.edit(behavior.group.id, Some(behavior.id)))
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
