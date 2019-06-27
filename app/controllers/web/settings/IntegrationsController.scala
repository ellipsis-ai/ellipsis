package controllers.web.settings

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.{ReAuthable, RemoteAssets}
import javax.inject.Inject
import json.Formatting._
import json._
import json.web.settings.IntegrationListConfig
import models._
import models.accounts.oauth1application.OAuth1Application
import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.{User, UserTeamAccess}
import models.accounts.{OAuth2State, OAuthApi, OAuthApplication}
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.config.RequiredOAuthApiConfig
import models.silhouette.EllipsisEnv
import models.team.Team
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Result}
import play.filters.csrf.CSRF
import play.mvc.Http.Request
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
              oauthApis = (oauth1Apis ++ oauth2Apis).map(api => OAuthApiData.from(api, assets)),
              oauthApplications = (oauth1Applications ++ oauth2Applications).map(app => OAuthApplicationData.from(app)),
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
          maybeRequiredOAuthApplication: Option[RequiredOAuthApiConfig] <- (for {
            groupVersion <- maybeBehaviorGroupVersion
            nameInCode <- maybeRequiredNameInCode
          } yield {
            for {
              maybeOAuth1 <- dataService.requiredOAuth1ApiConfigs.findWithNameInCode(nameInCode, groupVersion)
              maybeOAuth2 <- dataService.requiredOAuth2ApiConfigs.findWithNameInCode(nameInCode, groupVersion)
            } yield {
              maybeOAuth1.orElse(maybeOAuth2)
            }
          }).getOrElse(Future.successful(None))
        } yield {
          teamAccess.maybeTargetTeam.map { team =>
            val newApplicationId = IDs.next
            val config = OAuthApplicationEditConfig(
              containerId = "applicationEditor",
              csrfToken = CSRF.getToken(request).map(_.value),
              isAdmin = teamAccess.isAdminAccess,
              teamId = team.id,
              apis = (oauth1Apis ++ oauth2Apis).map(ea => OAuthApiData.from(ea, assets)),
              oauth1CallbackUrl = controllers.routes.APIAccessController.linkCustomOAuth1Service(newApplicationId, None, None).absoluteURL(secure = true),
              oauth2CallbackUrl = controllers.routes.APIAccessController.linkCustomOAuth2Service(newApplicationId, None, None).absoluteURL(secure = true),
              authorizationUrl = None,
              requiresAuth = maybeRequiredOAuthApplication.flatMap(_.maybeApplication.map(_.api.requiresAuth)),
              mainUrl = controllers.routes.ApplicationController.index().absoluteURL(secure = true),
              applicationId = newApplicationId,
              applicationApiId = maybeRequiredOAuthApplication.map(_.api.id),
              applicationKey = maybeRequiredOAuthApplication.flatMap(_.maybeApplication.map(_.key)),
              applicationSecret = maybeRequiredOAuthApplication.flatMap(_.maybeApplication.map(_.secret)),
              recommendedScope = maybeRequiredOAuthApplication.flatMap(_.maybeRecommendedScope),
              requiredNameInCode = maybeRequiredNameInCode,
              behaviorGroupId = maybeBehaviorGroupId,
              behaviorId = maybeBehaviorId,
              sharedTokenUser = None
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
            Ok(views.html.web.settings.integrations.edit(viewConfig(Some(teamAccess)), "Add an API configuration", dataRoute))
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
                                    key: String,
                                    secret: String,
                                    maybeScope: Option[String],
                                    teamId: String,
                                    maybeBehaviorGroupId: Option[String],
                                    maybeBehaviorId: Option[String],
                                    maybeIsShared: Option[String],
                                    maybeRequiredNameInCode: Option[String],
                                    isForOAuth1: Boolean
                                  ) {
    val isShared: Boolean = maybeIsShared.contains("on")
  }

  private val saveForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "name" -> nonEmptyText,
      "apiId" -> nonEmptyText,
      "key" -> nonEmptyText,
      "secret" -> nonEmptyText,
      "scope" -> optional(nonEmptyText),
      "teamId" -> nonEmptyText,
      "behaviorGroupId" -> optional(nonEmptyText),
      "behaviorId" -> optional(nonEmptyText),
      "isShared" -> optional(nonEmptyText),
      "requiredNameInCode" -> optional(nonEmptyText),
      "isForOAuth1" -> boolean
    )(OAuthApplicationInfo.apply)(OAuthApplicationInfo.unapply)
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
      } yield {
        val isShared = isAdmin && info.isShared
        val instance = OAuth1Application(info.id, info.name, api, info.key, info.secret, info.maybeScope, team.id, isShared)
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
            val maybeApplicationData = maybeApplication.map(OAuthApplicationData.from)
            dataService.requiredOAuth1ApiConfigs.maybeCreateFor(
              RequiredOAuthApiConfigData(None, None, info.apiId, info.maybeScope, nameInCode, maybeApplicationData),
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
      } yield {
        val isShared = isAdmin && info.isShared
        val instance = OAuth2Application(info.id, info.name, api, info.key, info.secret, info.maybeScope, team.id, isShared)
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
            val maybeApplicationData = maybeApplication.map(OAuthApplicationData.from)
            dataService.requiredOAuth2ApiConfigs.maybeCreateFor(
              RequiredOAuthApiConfigData(None, None, info.apiId, info.maybeScope, nameInCode, maybeApplicationData),
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

  def doShareMyOAuth1Token(applicationId: String, maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      maybeApplication <- dataService.oauth1Applications.find(applicationId)
      _ <- maybeApplication.map { app =>
        dataService.oauth1TokenShares.ensureFor(user, app)
      }.getOrElse(Future.successful({}))
    } yield {
      Redirect(routes.IntegrationsController.edit(applicationId).absoluteURL(secure = true))
    }
  }

  def shareMyOAuth1Token(applicationId: String, maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      maybeApplication <- dataService.oauth1Applications.find(applicationId)
    } yield {
      maybeApplication.map { application =>
        Ok(views.html.apiaccess.shareMyOAuthToken(viewConfig(Some(teamAccess)), teamAccess.maybeTargetTeam, application, routes.IntegrationsController.doShareMyOAuth1Token))
      }.getOrElse(NotFound(""))
    }
  }

  def doShareMyOAuth2Token(applicationId: String, maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      maybeApplication <- dataService.oauth2Applications.find(applicationId)
      _ <- maybeApplication.map { app =>
        dataService.oauth2TokenShares.ensureFor(user, app)
      }.getOrElse(Future.successful({}))
    } yield {
      Redirect(routes.IntegrationsController.edit(applicationId).absoluteURL(secure = true))
    }
  }

  def resetSharedOAuthToken(applicationId: String, maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      maybeOAuth1Application <- dataService.oauth1Applications.find(applicationId)
      maybeOAuth2Application <- dataService.oauth2Applications.find(applicationId)
      _ <- maybeOAuth1Application.map { app =>
        dataService.oauth1TokenShares.removeFor(user, app, teamAccess.maybeTargetTeam)
      }.getOrElse(Future.successful({}))
      _ <- maybeOAuth2Application.map { app =>
        dataService.oauth2TokenShares.removeFor(user, app, teamAccess.maybeTargetTeam)
      }.getOrElse(Future.successful({}))
    } yield {
      Ok(Json.obj())
    }
  }

  def shareMyOAuth2Token(applicationId: String, maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      maybeApplication <- dataService.oauth2Applications.find(applicationId)
    } yield {
      maybeApplication.map { application =>
        Ok(views.html.apiaccess.shareMyOAuthToken(viewConfig(Some(teamAccess)), teamAccess.maybeTargetTeam, application, routes.IntegrationsController.doShareMyOAuth2Token))
      }.getOrElse(NotFound(""))
    }
  }

  private def editConfigFor(
                             application: OAuthApplication,
                             teamAccess: UserTeamAccess,
                             team: Team,
                             oauthApis: Seq[OAuthApi],
                             isAdmin: Boolean,
                             maybeSharedTokenUserData: Option[UserData]
                           )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): OAuthApplicationEditConfig = {
    OAuthApplicationEditConfig(
      containerId = "applicationEditor",
      csrfToken = CSRF.getToken(request).map(_.value),
      isAdmin = teamAccess.isAdminAccess,
      teamId = team.id,
      apis = oauthApis.map(ea => OAuthApiData.from(ea, assets)),
      applicationKey = Some(application.key),
      applicationSecret = Some(application.secret),
      oauth1CallbackUrl = controllers.routes.APIAccessController.linkCustomOAuth1Service(application.id, None, None).absoluteURL(secure = true),
      oauth2CallbackUrl = controllers.routes.APIAccessController.linkCustomOAuth2Service(application.id, None, None).absoluteURL(secure = true),
      authorizationUrl = application.maybeTokenSharingAuthUrl,
      mainUrl = controllers.routes.ApplicationController.index().absoluteURL(secure = true),
      applicationId = application.id,
      applicationName = Some(application.name),
      applicationApiId = Some(application.api.id),
      applicationScope = application.maybeScope,
      applicationSaved = true,
      applicationShared = application.isShared,
      applicationCanBeShared = isAdmin,
      sharedTokenUser = maybeSharedTokenUserData
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
      maybeSharedOAuth1TokenUser <- maybeOAuth1Application.flatMap { oauth1Application =>
        teamAccess.maybeTargetTeam.map { team =>
          dataService.oauth1TokenShares.findFor(team, oauth1Application).flatMap { maybeShare =>
            maybeShare.map { share =>
              dataService.users.find(share.userId)
            }.getOrElse(Future.successful(None))
          }
        }
      }.getOrElse(Future.successful(None))
      maybeSharedOAuth1TokenUserData <- maybeSharedOAuth1TokenUser.map { user =>
        dataService.users.userDataFor(user, teamAccess.maybeTargetTeam.getOrElse(teamAccess.loggedInTeam)).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeOAuth2Application <- teamAccess.maybeTargetTeam.map { team =>
        dataService.oauth2Applications.find(id).map { maybeApp =>
          maybeApp.filter(_.teamId == team.id)
        }
      }.getOrElse(Future.successful(None))
      maybeSharedOAuth2TokenUser <- maybeOAuth2Application.flatMap { oauth2Application =>
        teamAccess.maybeTargetTeam.map { team =>
          dataService.oauth2TokenShares.findFor(team, oauth2Application).flatMap { maybeShare =>
            maybeShare.map { share =>
              dataService.users.find(share.userId)
            }.getOrElse(Future.successful(None))
          }
        }
      }.getOrElse(Future.successful(None))
      maybeSharedOAuth2TokenUserData <- maybeSharedOAuth2TokenUser.map { user =>
        dataService.users.userDataFor(user, teamAccess.maybeTargetTeam.getOrElse(teamAccess.loggedInTeam)).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield {
      val maybeOAuthApplication: Option[OAuthApplication] = maybeOAuth1Application.orElse(maybeOAuth2Application)
      render {
        case Accepts.JavaScript() => {
          (for {
            team <- teamAccess.maybeTargetTeam
            config <- maybeOAuthApplication.map(editConfigFor(_, teamAccess, team, oauth1Apis ++ oauth2Apis, isAdmin, maybeSharedOAuth1TokenUserData.orElse(maybeSharedOAuth2TokenUserData)))
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
            _ <- maybeOAuthApplication
          } yield {
            val dataRoute = routes.IntegrationsController.edit(id, maybeTeamId)
            Ok(views.html.web.settings.integrations.edit(viewConfig(Some(teamAccess)), "Edit API configuration", dataRoute))
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

  case class DeleteInfo(id: String, teamId: String)

  private val deleteForm = Form(
    mapping(
      "id" -> nonEmptyText,
      "teamId" -> nonEmptyText
    )(DeleteInfo.apply)(DeleteInfo.unapply)
  )

  def delete = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    deleteForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeTeam <- dataService.teams.find(info.teamId, user)
          result <- maybeTeam.map { team =>
            deleteApplicationForTeam(info.id, team)
          }.getOrElse {
            Future.successful(NotFound("Team not found"))
          }
        } yield result
      }
    )
  }

  private def deleteApplicationForTeam(applicationId: String, team: Team): Future[Result] = {
    for {
      maybeOAuth1Application <- dataService.oauth1Applications.find(applicationId).map(_.filter(_.teamId == team.id))
      maybeOAuth2Application <- if (maybeOAuth1Application.isDefined) {
        Future.successful(None)
      } else {
        dataService.oauth2Applications.find(applicationId).map(_.filter(_.teamId == team.id))
      }
      maybeDeleted <- {
        maybeOAuth1Application.map { oauth1App =>
          dataService.oauth1Applications.delete(oauth1App).map(Some(_))
        }.orElse {
          maybeOAuth2Application.map { oauth2App =>
            dataService.oauth2Applications.delete(oauth2App).map(Some(_))
          }
        }.getOrElse {
          Future.successful(None)
        }
      }
    } yield {
      maybeDeleted.map { isDeleted =>
        if (isDeleted) {
          Redirect(routes.IntegrationsController.list(Some(team.id)))
        } else {
          BadGateway("Error deleting integration")
        }
      }.getOrElse {
        NotFound("Integration not found")
      }
    }
  }
}
