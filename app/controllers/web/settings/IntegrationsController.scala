package controllers.web.settings

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{ReAuthable, RemoteAssets}
import javax.inject.Inject
import json._
import json.Formatting._
import json.web.settings.IntegrationList
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
          apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
          applications <- teamAccess.maybeTargetTeam.map { team =>
            dataService.oauth2Applications.allEditableFor(team)
          }.getOrElse(Future.successful(Seq()))
          awsConfigs <- teamAccess.maybeTargetTeam.map { team =>
            dataService.awsConfigs.allFor(team)
          }.getOrElse(Future.successful(Seq()))
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val config = IntegrationList(
              containerId = "applicationList",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamId = team.id,
              apis = apis.map(api => OAuth2ApiData.from(api, assets)),
              applications = applications.map(app => OAuth2ApplicationData.from(app)),
              awsConfigs = awsConfigs.map(AWSConfigData.from)
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "web/settings/integrations/list", Json.toJson(config)))
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
          apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
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
            val config = OAuth2ApplicationEditConfig(
              containerId = "applicationEditor",
              csrfToken = CSRF.getToken(request).map(_.value),
              isAdmin = teamAccess.isAdminAccess,
              teamId = team.id,
              apis = apis.map(ea => OAuth2ApiData.from(ea, assets)),
              callbackUrl = controllers.routes.APIAccessController.linkCustomOAuth2Service(newApplicationId, None, None, None, None).absoluteURL(secure = true),
              mainUrl = controllers.routes.ApplicationController.index().absoluteURL(secure = true),
              applicationId = newApplicationId,
              applicationApiId = maybeRequiredOAuth2Application.map(_.api.id),
              recommendedScope = maybeRequiredOAuth2Application.flatMap(_.maybeRecommendedScope),
              requiredNameInCode = maybeRequiredNameInCode,
              behaviorGroupId = maybeBehaviorGroupId,
              behaviorId = maybeBehaviorId
            )
            Ok(views.js.shared.pageConfig(viewConfig(Some(teamAccess)), "settings/integrations/editor", Json.toJson(config)))
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
            val dataRoute = controllers.web.settings.routes.IntegrationsController.add(maybeTeamId, maybeBehaviorGroupId, maybeBehaviorId, maybeRequiredNameInCode)
            Ok(views.html.web.settings.integrations.edit(viewConfig(Some(teamAccess)), "New Integration", dataRoute))
          }.getOrElse {
            NotFound(s"Team not found: ${maybeTeamId}")
          }
        }
      }
    }
  }
}
