package controllers.web.settings

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{ReAuthable, RemoteAssets}
import javax.inject.Inject
import json._
import json.Formatting._
import json.web.settings.IntegrationListConfig
import models._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.libs.json.Json
import play.filters.csrf.CSRF
import services.DataService

import scala.concurrent.{ExecutionContext, Future}


class IntegrationsController @Inject() (
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
          oauth1Apis <- dataService.oauth1Apis.allFor(teamAccess.maybeTargetTeam)
          oauth1Applications <- teamAccess.maybeTargetTeam.map { team =>
            dataService.oauth1Applications.allEditableFor(team)
          }.getOrElse(Future.successful(Seq()))
          oauth2Apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
          oauth2Applications <- teamAccess.maybeTargetTeam.map { team =>
            dataService.oauth2Applications.allEditableFor(team)
          }.getOrElse(Future.successful(Seq()))
          awsConfigs <- teamAccess.maybeTargetTeam.map { team =>
            dataService.awsConfigs.allFor(team)
          }.getOrElse(Future.successful(Seq()))
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val config = IntegrationListConfig(
              containerId = "applicationList",
              csrfToken = CSRF.getToken(request).map(_.value),
              isAdmin = teamAccess.isAdminAccess,
              teamId = team.id,
              oauth1Apis = oauth1Apis.map(api => OAuth1ApiData.from(api, assets)),
              oauth1Applications = oauth1Applications.map(app => OAuth1ApplicationData.from(app)),
              oauth2Apis = oauth2Apis.map(api => OAuth2ApiData.from(api, assets)),
              oauth2Applications = oauth2Applications.map(app => OAuth2ApplicationData.from(app)),
              awsConfigs = awsConfigs.map(AWSConfigData.from)
            )
            Ok(views.js.shared.webpackLoader(
              viewConfig(Some(teamAccess)),
              "IntegrationListConfig",
              "integrationList",
              Json.toJson(config)
            ))
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
            val dataRoute = routes.IntegrationsController.list(maybeTeamId)
            Ok(views.html.web.settings.integrations.list(viewConfig(Some(teamAccess)), dataRoute))
          }.getOrElse {
            NotFound("Team not found")
          }
        }
      }
    }
  }

  def add(
           maybeTeamId: Option[String],
           maybeBehaviorGroupId: Option[String],
           maybeBehaviorId: Option[String],
           maybeRequiredNameInCode: Option[String]
         ) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          oauth1Apis <- dataService.oauth1Apis.allFor(teamAccess.maybeTargetTeam)
          oauth2Apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
          maybeBehaviorGroup <- maybeBehaviorGroupId.map { groupId =>
            dataService.behaviorGroups.find(groupId, user)
          }.getOrElse(Future.successful(None))
          maybeBehaviorGroupVersion <- maybeBehaviorGroup.map { group =>
            dataService.behaviorGroups.maybeCurrentVersionFor(group)
          }.getOrElse(Future.successful(None))
          maybeRequiredOAuth2Application <- (for {
            groupVersion <- maybeBehaviorGroupVersion
            nameInCode <- maybeRequiredNameInCode
          } yield {
            dataService.requiredOAuth2ApiConfigs.findWithNameInCode(nameInCode, groupVersion)
          }).getOrElse(Future.successful(None))
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val newApplicationId = IDs.next
            val config = OAuthApplicationEditConfig(
              containerId = "applicationEditor",
              csrfToken = CSRF.getToken(request).map(_.value),
              isAdmin = teamAccess.isAdminAccess,
              teamId = team.id,
              oauth1Config = OAuth1ApplicationEditConfig(
                apis = oauth1Apis.map(ea => OAuth1ApiData.from(ea, assets)),
                callbackUrl = controllers.routes.APIAccessController.linkCustomOAuth1Service(newApplicationId, None, None).absoluteURL(secure = true)
              ),
              oauth2Config = OAuth2ApplicationEditConfig(
                apis = oauth2Apis.map(ea => OAuth2ApiData.from(ea, assets)),
                callbackUrl = controllers.routes.APIAccessController.linkCustomOAuth2Service(newApplicationId, None, None, None, None).absoluteURL(secure = true),
                requiresAuth = maybeRequiredOAuth2Application.map(_.api.grantType.requiresAuth),
                recommendedScope = maybeRequiredOAuth2Application.flatMap(_.maybeRecommendedScope)
              ),
              mainUrl = controllers.routes.ApplicationController.index().absoluteURL(secure = true),
              applicationId = newApplicationId,
              applicationApiId = maybeRequiredOAuth2Application.map(_.api.id),
              requiredNameInCode = maybeRequiredNameInCode,
              behaviorGroupId = maybeBehaviorGroupId,
              behaviorId = maybeBehaviorId
            )
            Ok(views.js.shared.webpackLoader(
              viewConfig(Some(teamAccess)),
              "IntegrationEditorConfig",
              "integrationEditor",
              Json.toJson(config)
            ))
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
            val dataRoute = routes.IntegrationsController.add(maybeTeamId, maybeBehaviorGroupId, maybeBehaviorId, maybeRequiredNameInCode)
            Ok(views.html.web.settings.oauth2application.edit(viewConfig(Some(teamAccess)), "Add an API configuration", dataRoute))
          }.getOrElse {
            NotFound(s"Team not found: ${maybeTeamId}")
          }
        }
      }
    }
  }

}
