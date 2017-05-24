package controllers

import javax.inject.Inject

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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OAuth2ApplicationController @Inject() (
                                              val messagesApi: MessagesApi,
                                              val silhouette: Silhouette[EllipsisEnv],
                                              val dataService: DataService,
                                              val configuration: Configuration
                                            ) extends ReAuthable {

  def list(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
          applications <- teamAccess.maybeTargetTeam.map { team =>
            dataService.oauth2Applications.allFor(team)
          }.getOrElse(Future.successful(Seq()))
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val config = OAuth2ApplicationListConfig(
              containerId = "applicationList",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamId = team.id,
              apis = apis.map(api => OAuth2ApiData.from(api)),
              applications = applications.map(app => OAuth2ApplicationData.from(app))
            )
            Ok(views.js.shared.pageConfig("config/oauth2application/list", Json.toJson(config)))
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

  def newApp(maybeApiId: Option[String], maybeRecommendedScope: Option[String], maybeTeamId: Option[String], maybeBehaviorId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val newApplicationId = IDs.next
            val config = OAuth2ApplicationEditConfig(
              containerId = "applicationEditor",
              csrfToken = CSRF.getToken(request).map(_.value),
              teamId = team.id,
              apis = apis.map(OAuth2ApiData.from),
              callbackUrl = routes.APIAccessController.linkCustomOAuth2Service(newApplicationId, None, None, None, None).absoluteURL(secure = true),
              mainUrl = routes.ApplicationController.index().absoluteURL(secure = true),
              applicationId = newApplicationId,
              applicationApiId = maybeApiId,
              recommendedScope = maybeRecommendedScope,
              behaviorId = maybeBehaviorId
            )
            Ok(views.js.shared.pageConfig("config/oauth2application/edit", Json.toJson(config)))
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
            val dataRoute = routes.OAuth2ApplicationController.newApp(maybeApiId, maybeRecommendedScope, maybeTeamId, maybeBehaviorId)
            Ok(views.html.oauth2application.edit(viewConfig(Some(teamAccess)), "Add an API application", dataRoute))
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
      apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
      maybeApplication <- teamAccess.maybeTargetTeam.map { team =>
        dataService.oauth2Applications.find(id)
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
              apis = apis.map(OAuth2ApiData.from),
              callbackUrl = routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None, None, None).absoluteURL(secure = true),
              mainUrl = routes.ApplicationController.index().absoluteURL(secure = true),
              applicationId = application.id,
              applicationName = Some(application.name),
              requiresAuth = Some(application.api.grantType.requiresAuth),
              applicationClientId = Some(application.clientId),
              applicationClientSecret = Some(application.clientSecret),
              applicationScope = application.maybeScope,
              applicationApiId = Some(application.api.id),
              applicationSaved = true
            )
            Ok(views.js.shared.pageConfig("config/oauth2application/edit", Json.toJson(config)))
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
                                    maybeBehaviorId: Option[String]
                                  )

  private val saveForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "name" -> nonEmptyText,
      "apiId" -> nonEmptyText,
      "clientId" -> nonEmptyText,
      "clientSecret" -> nonEmptyText,
      "scope" -> optional(nonEmptyText),
      "teamId" -> nonEmptyText,
      "behaviorId" -> optional(nonEmptyText)
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
          maybeApi <- dataService.oauth2Apis.find(info.apiId)
          maybeApplication <- (for {
            api <- maybeApi
            team <- maybeTeam
          } yield {
            val instance = OAuth2Application(info.id, info.name, api, info.clientId, info.clientSecret, info.maybeScope, info.teamId)
            dataService.oauth2Applications.save(instance).map(Some(_))
          }).getOrElse(Future.successful(None))
          maybeBehaviorVersion <- info.maybeBehaviorId.map { behaviorId =>
            dataService.behaviors.find(behaviorId, user).flatMap { maybeBehavior =>
              maybeBehavior.map { behavior =>
                dataService.behaviors.maybeCurrentVersionFor(behavior)
              }.getOrElse(Future.successful(None))
            }
          }.getOrElse(Future.successful(None))
          requireOAuth2Applications <- (for {
            behaviorVersion <- maybeBehaviorVersion
            group <- behaviorVersion.behavior.maybeGroup
            api <- maybeApi
          } yield {
            dataService.requiredOAuth2ApiConfigs.allFor(api, group)
          }).getOrElse(Future.successful(Seq()))
          _ <- Future.sequence {
            requireOAuth2Applications.
              filter(_.maybeApplication.isEmpty).
              filter(_.maybeRecommendedScope == maybeApplication.flatMap(_.maybeScope)).
              map { ea =>
                dataService.requiredOAuth2ApiConfigs.save(ea.copy(maybeApplication = maybeApplication))
              }
          }
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
