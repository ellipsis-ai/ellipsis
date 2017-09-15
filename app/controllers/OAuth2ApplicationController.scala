package controllers

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import json.Formatting._
import json._
import models._
import models.accounts.oauth2application.OAuth2Application
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.filters.csrf.CSRF
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class OAuth2ApplicationController @Inject() (
                                              val silhouette: Silhouette[EllipsisEnv],
                                              val dataService: DataService,
                                              val configuration: Configuration,
                                              val assetsProvider: Provider[RemoteAssets],
                                              implicit val ec: ExecutionContext
                                            ) extends ReAuthable {

  def list(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
          applications <- teamAccess.maybeTargetTeam.map { team =>
            dataService.oauth2Applications.allEditableFor(team)
          }.getOrElse(Future.successful(Seq()))
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val config = OAuth2ApplicationListConfig(
              containerId = "applicationList",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamId = team.id,
              apis = apis.map(api => OAuth2ApiData.from(api, assets)),
              applications = applications.map(app => OAuth2ApplicationData.from(app))
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "config/oauth2application/list", Json.toJson(config)))
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
            val dataRoute = routes.OAuth2ApplicationController.list(maybeTeamId)
            Ok(views.html.oauth2application.list(viewConfig(Some(teamAccess)), dataRoute))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
    }
  }

  def newApp(maybeTeamId: Option[String], maybeBehaviorId: Option[String], maybeRequiredNameInCode: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
          maybeBehavior <- maybeBehaviorId.map { behaviorId =>
            dataService.behaviors.findWithoutAccessCheck(behaviorId)
          }.getOrElse(Future.successful(None))
          maybeBehaviorVersion <- maybeBehavior.map { behavior =>
            dataService.behaviors.maybeCurrentVersionFor(behavior)
          }.getOrElse(Future.successful(None))
          maybeRequiredOAuth2Application <- (for {
            groupVersion <- maybeBehaviorVersion.map(_.groupVersion)
            nameInCode <- maybeRequiredNameInCode
          } yield {
            dataService.requiredOAuth2ApiConfigs.findWithNameInCode(nameInCode, groupVersion)
          }).getOrElse(Future.successful(None))
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val newApplicationId = IDs.next
            val config = OAuth2ApplicationEditConfig(
              containerId = "applicationEditor",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamId = team.id,
              apis = apis.map(ea => OAuth2ApiData.from(ea, assets)),
              callbackUrl = routes.APIAccessController.linkCustomOAuth2Service(newApplicationId, None, None, None, None).absoluteURL(secure = true),
              mainUrl = routes.ApplicationController.index().absoluteURL(secure = true),
              applicationId = newApplicationId,
              applicationApiId = maybeRequiredOAuth2Application.map(_.api.id),
              recommendedScope = maybeRequiredOAuth2Application.flatMap(_.maybeRecommendedScope),
              requiredNameInCode = maybeRequiredNameInCode,
              behaviorId = maybeBehaviorId
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "config/oauth2application/edit", Json.toJson(config)))
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
            val dataRoute = routes.OAuth2ApplicationController.newApp(maybeTeamId, maybeBehaviorId, maybeRequiredNameInCode)
            Ok(views.html.oauth2application.edit(viewConfig(Some(teamAccess)), "Add an API application", dataRoute))
          }.getOrElse {
            NotFound(s"Team not found: ${maybeTeamId}")
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
      apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
      maybeApplication <- teamAccess.maybeTargetTeam.map { team =>
        dataService.oauth2Applications.find(id).map { maybeApp =>
          maybeApp.filter(_.teamId == team.id)
        }
      }.getOrElse(Future.successful(None))
    } yield {
      render {
        case Accepts.JavaScript() => {
          (for {
            team <- teamAccess.maybeTargetTeam
            application <- maybeApplication
          } yield {
            val config = OAuth2ApplicationEditConfig(
              containerId = "applicationEditor",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamId = team.id,
              apis = apis.map(ea => OAuth2ApiData.from(ea, assets)),
              callbackUrl = routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None, None, None).absoluteURL(secure = true),
              mainUrl = routes.ApplicationController.index().absoluteURL(secure = true),
              applicationId = application.id,
              applicationName = Some(application.name),
              requiresAuth = Some(application.api.grantType.requiresAuth),
              applicationClientId = Some(application.clientId),
              applicationClientSecret = Some(application.clientSecret),
              applicationScope = application.maybeScope,
              applicationApiId = Some(application.api.id),
              applicationSaved = true,
              applicationShared = application.isShared,
              applicationCanBeShared = isAdmin
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "config/oauth2application/edit", Json.toJson(config)))
          }).getOrElse {
            NotFound("Unknown application")
          }
        }
        case Accepts.Html() => {
          (for {
            _ <- teamAccess.maybeTargetTeam
            _ <- maybeApplication
          } yield {
            val dataRoute = routes.OAuth2ApplicationController.edit(id, maybeTeamId)
            Ok(views.html.oauth2application.edit(viewConfig(Some(teamAccess)), "Edit API application", dataRoute))
          }).getOrElse {
            NotFound(
              views.html.error.notFound(
                viewConfig(Some(teamAccess)),
                Some("OAuth2 application not found"),
                Some("The OAuth2 application you are trying to access could not be found."),
                Some(reAuthLinkFor(request, None))
              ))
          }
        }
      }
    }
  }

  case class OAuth2ApplicationInfo(
                                    id: String,
                                    name: String,
                                    apiId: String,
                                    clientId: String,
                                    clientSecret: String,
                                    maybeScope: Option[String],
                                    teamId: String,
                                    maybeBehaviorId: Option[String],
                                    maybeIsShared: Option[String],
                                    maybeRequiredNameInCode: Option[String]
                                  ) {
    val isShared: Boolean = maybeIsShared.contains("on")
  }

  private val saveForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "name" -> nonEmptyText,
      "apiId" -> nonEmptyText,
      "clientId" -> nonEmptyText,
      "clientSecret" -> nonEmptyText,
      "scope" -> optional(nonEmptyText),
      "teamId" -> nonEmptyText,
      "behaviorId" -> optional(nonEmptyText),
      "isShared" -> optional(nonEmptyText),
      "requiredNameInCode" -> optional(nonEmptyText)
    )(OAuth2ApplicationInfo.apply)(OAuth2ApplicationInfo.unapply)
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
          isAdmin <- dataService.users.isAdmin(user)
          maybeApi <- dataService.oauth2Apis.find(info.apiId)
          maybeApplication <- (for {
            api <- maybeApi
            team <- maybeTeam
          } yield {
            val isShared = isAdmin && info.isShared
            val instance = OAuth2Application(info.id, info.name, api, info.clientId, info.clientSecret, info.maybeScope, info.teamId, isShared)
            dataService.oauth2Applications.save(instance).map(Some(_))
          }).getOrElse(Future.successful(None))
          maybeBehaviorVersion <- info.maybeBehaviorId.map { behaviorId =>
            dataService.behaviors.find(behaviorId, user).flatMap { maybeBehavior =>
              maybeBehavior.map { behavior =>
                dataService.behaviors.maybeCurrentVersionFor(behavior)
              }.getOrElse(Future.successful(None))
            }
          }.getOrElse(Future.successful(None))
          _ <- (for {
            nameInCode <- info.maybeRequiredNameInCode
            groupVersion <- maybeBehaviorVersion.map(_.groupVersion)
          } yield {
            dataService.requiredOAuth2ApiConfigs.findWithNameInCode(nameInCode, groupVersion).flatMap { maybeExisting =>
              maybeExisting.map { existing =>
                dataService.requiredOAuth2ApiConfigs.save(existing.copy(maybeApplication = maybeApplication))
              }.getOrElse {
                val maybeApplicationData = maybeApplication.map(OAuth2ApplicationData.from)
                dataService.requiredOAuth2ApiConfigs.maybeCreateFor(
                  RequiredOAuth2ApiConfigData(None, info.apiId, info.maybeScope, nameInCode, maybeApplicationData),
                  groupVersion
                )
              }
            }
          }).getOrElse(Future.successful({}))
        } yield {
          maybeApplication.map { application =>
            maybeBehaviorVersion.map { behaviorVersion =>
              val behavior = behaviorVersion.behavior
              Redirect(routes.BehaviorEditorController.edit(behavior.group.id, Some(behavior.id)))
            }.getOrElse {
              Redirect(routes.OAuth2ApplicationController.edit(application.id, Some(application.teamId)))
            }
          }.getOrElse {
            NotFound(s"Team not found: ${info.teamId}")
          }
        }
      }
    )
  }


}
