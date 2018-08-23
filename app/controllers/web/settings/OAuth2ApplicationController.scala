package controllers.web.settings

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{ReAuthable, RemoteAssets}
import json._
import json.Formatting._
import models._
import models.accounts.oauth2application.OAuth2Application
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
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

  def edit(id: String, maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      isAdmin <- dataService.users.isAdmin(user)
      oauth1Apis <- dataService.oauth1Apis.allFor(teamAccess.maybeTargetTeam)
      oauth2Apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
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
            val config = OAuthApplicationEditConfig(
              containerId = "applicationEditor",
              csrfToken = CSRF.getToken(request).map(_.value),
              isAdmin = teamAccess.isAdminAccess,
              teamId = team.id,
              oauth1Config = OAuth1ApplicationEditConfig(
                apis = oauth1Apis.map(ea => OAuth1ApiData.from(ea, assets)),
                callbackUrl = controllers.routes.APIAccessController.linkCustomOAuth1Service(application.id, None, None).absoluteURL(secure = true)
              ),
              oauth2Config = OAuth2ApplicationEditConfig(
                apis = oauth2Apis.map(ea => OAuth2ApiData.from(ea, assets)),
                callbackUrl = controllers.routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None, None, None).absoluteURL(secure = true),
                requiresAuth = Some(application.api.grantType.requiresAuth),
                applicationClientId = Some(application.clientId),
                applicationClientSecret = Some(application.clientSecret),
                applicationScope = application.maybeScope
              ),
              mainUrl = controllers.routes.ApplicationController.index().absoluteURL(secure = true),
              applicationId = application.id,
              applicationName = Some(application.name),
              applicationApiId = Some(application.api.id),
              applicationSaved = true,
              applicationShared = application.isShared,
              applicationCanBeShared = isAdmin
            )
            Ok(views.js.shared.webpackLoader(
              viewConfig(Some(teamAccess)),
              "IntegrationEditorConfig",
              "integrationEditor",
              Json.toJson(config)
            ))
          }).getOrElse {
            NotFound("Unknown configuration")
          }
        }
        case Accepts.Html() => {
          (for {
            _ <- teamAccess.maybeTargetTeam
            _ <- maybeApplication
          } yield {
            val dataRoute = routes.OAuth2ApplicationController.edit(id, maybeTeamId)
            Ok(views.html.web.settings.oauth2application.edit(viewConfig(Some(teamAccess)), "Edit API configuration", dataRoute))
          }).getOrElse {
            NotFound(
              views.html.error.notFound(
                viewConfig(Some(teamAccess)),
                Some("OAuth2 API configuration not found"),
                Some("The OAuth2 API configuration you are trying to access could not be found."),
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
                                    maybeBehaviorGroupId: Option[String],
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
      "behaviorGroupId" -> optional(nonEmptyText),
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
          maybeBehaviorGroup <- info.maybeBehaviorGroupId.map { groupId =>
            dataService.behaviorGroups.find(groupId, user)
          }.getOrElse(Future.successful(None))
          maybeBehaviorGroupVersion <- maybeBehaviorGroup.map { group =>
            dataService.behaviorGroups.maybeCurrentVersionFor(group)
          }.getOrElse(Future.successful(None))
          _ <- (for {
            nameInCode <- info.maybeRequiredNameInCode
            groupVersion <- maybeBehaviorGroupVersion
          } yield {
            dataService.requiredOAuth2ApiConfigs.findWithNameInCode(nameInCode, groupVersion).flatMap { maybeExisting =>
              maybeExisting.map { existing =>
                dataService.requiredOAuth2ApiConfigs.save(existing.copy(maybeApplication = maybeApplication))
              }.getOrElse {
                val maybeApplicationData = maybeApplication.map(OAuth2ApplicationData.from)
                dataService.requiredOAuth2ApiConfigs.maybeCreateFor(
                  RequiredOAuth2ApiConfigData(None, None, info.apiId, info.maybeScope, nameInCode, maybeApplicationData),
                  groupVersion
                )
              }
            }
          }).getOrElse(Future.successful({}))
        } yield {
          maybeApplication.map { application =>
            maybeBehaviorGroup.map { group =>
              Redirect(controllers.routes.BehaviorEditorController.edit(group.id, info.maybeBehaviorId))
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
