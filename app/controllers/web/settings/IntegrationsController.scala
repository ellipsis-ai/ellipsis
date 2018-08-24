package controllers.web.settings

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.{ReAuthable, RemoteAssets}
import javax.inject.Inject
import json._
import json.Formatting._
import json.web.settings.IntegrationListConfig
import models._
import models.accounts.oauth1api.OAuth1Api
import models.accounts.oauth1application.OAuth1Application
import models.accounts.oauth2api.OAuth2Api
import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.{User, UserTeamAccess}
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.silhouette.EllipsisEnv
import models.team.Team
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional}
import play.api.libs.json.Json
import play.api.mvc.AnyContent
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


  case class OAuthApplicationInfo(
                                    id: String,
                                    name: String,
                                    apiId: String,
                                    clientId: Option[String],
                                    clientSecret: Option[String],
                                    consumerKey: Option[String],
                                    consumerSecret: Option[String],
                                    maybeScope: Option[String],
                                    teamId: String,
                                    maybeBehaviorGroupId: Option[String],
                                    maybeBehaviorId: Option[String],
                                    maybeIsShared: Option[String],
                                    maybeRequiredNameInCode: Option[String]
                                  ) {
    val isShared: Boolean = maybeIsShared.contains("on")
    val isForOAuth1: Boolean = consumerKey.isDefined
  }

  private def checkOAuthInfo(info: OAuthApplicationInfo) = {
    (info.clientId.isDefined && info.clientSecret.isDefined) || (info.consumerKey.isDefined && info.consumerSecret.isDefined)
  }
  private val saveForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "name" -> nonEmptyText,
      "apiId" -> nonEmptyText,
      "clientId" -> optional(nonEmptyText),
      "clientSecret" -> optional(nonEmptyText),
      "consumerKey" -> optional(nonEmptyText),
      "consumerSecret" -> optional(nonEmptyText),
      "scope" -> optional(nonEmptyText),
      "teamId" -> nonEmptyText,
      "behaviorGroupId" -> optional(nonEmptyText),
      "behaviorId" -> optional(nonEmptyText),
      "isShared" -> optional(nonEmptyText),
      "requiredNameInCode" -> optional(nonEmptyText)
    )(OAuthApplicationInfo.apply)(OAuthApplicationInfo.unapply) verifying("Not valid for OAuth1 or OAuth2", checkOAuthInfo _)
  )

  private def saveOAuth1(
                          user: User,
                          maybeTeam: Option[Team],
                          isAdmin: Boolean,
                          maybeBehaviorGroup: Option[BehaviorGroup],
                          maybeBehaviorGroupVersion: Option[BehaviorGroupVersion],
                          info: OAuthApplicationInfo
                        ) = {
    for {
      maybeApi <- dataService.oauth1Apis.find(info.apiId)
      maybeApplication <- (for {
        api <- maybeApi
        team <- maybeTeam
        consumerKey <- info.consumerKey
        consumerSecret <- info.consumerSecret
      } yield {
        val isShared = isAdmin && info.isShared
        val instance = OAuth1Application(info.id, info.name, api, consumerKey, consumerSecret, team.id, isShared)
        dataService.oauth1Applications.save(instance).map(Some(_))
      }).getOrElse(Future.successful(None))
      _ <- (for {
        nameInCode <- info.maybeRequiredNameInCode
        groupVersion <- maybeBehaviorGroupVersion
      } yield {
        dataService.requiredOAuth1ApiConfigs.findWithNameInCode(nameInCode, groupVersion).flatMap { maybeExisting =>
          maybeExisting.map { existing =>
            dataService.requiredOAuth1ApiConfigs.save(existing.copy(maybeApplication = maybeApplication))
          }.getOrElse {
            val maybeApplicationData = maybeApplication.map(OAuth1ApplicationData.from)
            dataService.requiredOAuth1ApiConfigs.maybeCreateFor(
              RequiredOAuth1ApiConfigData(None, None, info.apiId, nameInCode, maybeApplicationData),
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
          Redirect(routes.IntegrationsController.edit(application.id, Some(application.teamId)))
        }
      }.getOrElse {
        NotFound(s"Team not found: ${info.teamId}")
      }
    }
  }

  private def saveOAuth2(
                          user: User,
                          maybeTeam: Option[Team],
                          isAdmin: Boolean,
                          maybeBehaviorGroup: Option[BehaviorGroup],
                          maybeBehaviorGroupVersion: Option[BehaviorGroupVersion],
                          info: OAuthApplicationInfo
                        ) = {
    for {
      maybeApi <- dataService.oauth2Apis.find(info.apiId)
      maybeApplication <- (for {
        api <- maybeApi
        team <- maybeTeam
        clientId <- info.clientId
        clientSecret <- info.clientSecret
      } yield {
        val isShared = isAdmin && info.isShared
        val instance = OAuth2Application(info.id, info.name, api, clientId, clientSecret, info.maybeScope, team.id, isShared)
        dataService.oauth2Applications.save(instance).map(Some(_))
      }).getOrElse(Future.successful(None))
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
          Redirect(routes.IntegrationsController.edit(application.id, Some(application.teamId)))
        }
      }.getOrElse {
        NotFound(s"Team not found: ${info.teamId}")
      }
    }
  }

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
          maybeBehaviorGroup <- info.maybeBehaviorGroupId.map { groupId =>
            dataService.behaviorGroups.find(groupId, user)
          }.getOrElse(Future.successful(None))
          maybeBehaviorGroupVersion <- maybeBehaviorGroup.map { group =>
            dataService.behaviorGroups.maybeCurrentVersionFor(group)
          }.getOrElse(Future.successful(None))
          result <- if (info.isForOAuth1) {
            saveOAuth1(user, maybeTeam, isAdmin, maybeBehaviorGroup, maybeBehaviorGroupVersion, info)
          } else {
            saveOAuth2(user, maybeTeam, isAdmin, maybeBehaviorGroup, maybeBehaviorGroupVersion, info)
          }
        } yield result
      }
    )
  }

  private def editConfigFor(
                             application: OAuth1Application,
                             teamAccess: UserTeamAccess,
                             team: Team,
                             oauth1Apis: Seq[OAuth1Api],
                             oauth2Apis: Seq[OAuth2Api],
                             isAdmin: Boolean
                           )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): OAuthApplicationEditConfig = {
    OAuthApplicationEditConfig(
      containerId = "applicationEditor",
      csrfToken = CSRF.getToken(request).map(_.value),
      isAdmin = teamAccess.isAdminAccess,
      teamId = team.id,
      oauth1Config = OAuth1ApplicationEditConfig(
        apis = oauth1Apis.map(ea => OAuth1ApiData.from(ea, assets)),
        callbackUrl = controllers.routes.APIAccessController.linkCustomOAuth1Service(application.id, None, None).absoluteURL(secure = true),
        applicationConsumerKey = Some(application.consumerKey),
        applicationConsumerSecret = Some(application.consumerSecret)
      ),
      oauth2Config = OAuth2ApplicationEditConfig(
        apis = oauth2Apis.map(ea => OAuth2ApiData.from(ea, assets)),
        callbackUrl = controllers.routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None, None, None).absoluteURL(secure = true),
        requiresAuth = None,
        applicationClientId = None,
        applicationClientSecret = None,
        applicationScope = None
      ),
      mainUrl = controllers.routes.ApplicationController.index().absoluteURL(secure = true),
      applicationId = application.id,
      applicationName = Some(application.name),
      applicationApiId = Some(application.api.id),
      applicationSaved = true,
      applicationShared = application.isShared,
      applicationCanBeShared = isAdmin
    )
  }

  private def editConfigFor(
                             application: OAuth2Application,
                             teamAccess: UserTeamAccess,
                             team: Team,
                             oauth1Apis: Seq[OAuth1Api],
                             oauth2Apis: Seq[OAuth2Api],
                             isAdmin: Boolean
                           )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): OAuthApplicationEditConfig = {
    OAuthApplicationEditConfig(
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
  }

  def edit(id: String, maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      isAdmin <- dataService.users.isAdmin(user)
      oauth1Apis <- dataService.oauth1Apis.allFor(teamAccess.maybeTargetTeam)
      oauth2Apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
      maybeOAuth1Application <- teamAccess.maybeTargetTeam.map { team =>
        dataService.oauth1Applications.find(id).map { maybeApp =>
          maybeApp.filter(_.teamId == team.id)
        }
      }.getOrElse(Future.successful(None))
      maybeOAuth2Application <- teamAccess.maybeTargetTeam.map { team =>
        dataService.oauth2Applications.find(id).map { maybeApp =>
          maybeApp.filter(_.teamId == team.id)
        }
      }.getOrElse(Future.successful(None))
    } yield {
      val maybeApplication = maybeOAuth1Application.orElse(maybeOAuth2Application)
      render {
        case Accepts.JavaScript() => {
          (for {
            team <- teamAccess.maybeTargetTeam
            config <- maybeOAuth1Application.map(editConfigFor(_, teamAccess, team, oauth1Apis, oauth2Apis, isAdmin)).orElse {
              maybeOAuth2Application.map(editConfigFor(_, teamAccess, team, oauth1Apis, oauth2Apis, isAdmin))
            }
          } yield {
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
            val dataRoute = routes.IntegrationsController.edit(id, maybeTeamId)
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

}
