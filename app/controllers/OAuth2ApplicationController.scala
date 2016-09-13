package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import json._
import models.bots.config.RequiredOAuth2ApiConfigQueries
import models._
import models.accounts._
import models.accounts.oauth2application.OAuth2Application
import models.bots._
import models.silhouette.EllipsisEnv
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OAuth2ApplicationController @Inject() (
                                              val messagesApi: MessagesApi,
                                              val silhouette: Silhouette[EllipsisEnv],
                                              val dataService: DataService
                                            ) extends ReAuthable {

  def list(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
      applications <- teamAccess.maybeTargetTeam.map { team =>
        dataService.oauth2Applications.allFor(team)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      teamAccess.maybeTargetTeam.map { team =>
        Ok(
          views.html.listOAuth2Applications(
            teamAccess,
            apis.map(api => OAuth2ApiData.from(api)),
            applications.map(app => OAuth2ApplicationData.from(app))
          )
        )
      }.getOrElse{
        NotFound("Team not accessible")
      }
    }
  }

  def newApp(maybeRequiredOAuth2ApiConfigId: Option[String], maybeTeamId: Option[String], maybeBehaviorId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- DBIO.from(dataService.users.teamAccessFor(user, maybeTeamId))
      apis <- DBIO.from(dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam))
      maybeRequiredOAuth2ApiConfig <- maybeRequiredOAuth2ApiConfigId.map { id =>
        RequiredOAuth2ApiConfigQueries.find(id)
      }.getOrElse(DBIO.successful(None))
    } yield {
      teamAccess.maybeTargetTeam.map { team =>
        val maybeApiId = maybeRequiredOAuth2ApiConfig.map(_.api.id)
        val maybeRecommendedScope = maybeRequiredOAuth2ApiConfig.flatMap(_.maybeRecommendedScope)
        Ok(views.html.newOAuth2Application(
          teamAccess,
          apis.map(api => OAuth2ApiData.from(api)),
          IDs.next,
          maybeApiId,
          maybeRecommendedScope,
          maybeRequiredOAuth2ApiConfigId,
          maybeBehaviorId)
        )
      }.getOrElse {
        NotFound("Team not accessible")
      }
    }

    dataService.run(action)
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
      (for {
        team <- teamAccess.maybeTargetTeam
        application <- maybeApplication
      } yield {
        Ok(views.html.editOAuth2Application(teamAccess, apis.map(api => OAuth2ApiData.from(api)), application))
      }).getOrElse {
        NotFound(
          views.html.notFound(
            Some(teamAccess),
            Some("OAuth2 application not found"),
            Some("The OAuth2 application you are trying to access could not be found."),
            Some(reAuthLinkFor(request, None))
          ))
      }
    }
  }

  case class OAuth2ApplicationInfo(
                                    id: String,
                                    name: String,
                                    apiId: String,
                                    maybeRequiredOAuth2ApiConfigId: Option[String],
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
      "requiredOAuth2ApiConfigId" -> optional(nonEmptyText),
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
        val action = for {
          maybeTeam <- DBIO.from(dataService.teams.find(info.teamId, user))
          maybeApi <- DBIO.from(dataService.oauth2Apis.find(info.apiId))
          maybeApplication <- (for {
            api <- maybeApi
            team <- maybeTeam
          } yield {
            val instance = OAuth2Application(info.id, info.name, api, info.clientId, info.clientSecret, info.maybeScope, info.teamId)
            DBIO.from(dataService.oauth2Applications.save(instance)).map(Some(_))
          }).getOrElse(DBIO.successful(None))
          maybeBehaviorVersion <- info.maybeBehaviorId.map { behaviorId =>
            DBIO.from(dataService.behaviors.find(behaviorId, user)).flatMap { maybeBehavior =>
              maybeBehavior.map { behavior =>
                DBIO.from(dataService.behaviors.maybeCurrentVersionFor(behavior))
              }.getOrElse(DBIO.successful(None))
            }
          }.getOrElse(DBIO.successful(None))
          maybeRequired <- info.maybeRequiredOAuth2ApiConfigId.map { requiredId =>
            RequiredOAuth2ApiConfigQueries.find(requiredId).flatMap { maybeExisting =>
              maybeExisting.map { existing =>
                RequiredOAuth2ApiConfigQueries.save(existing.copy(maybeApplication = maybeApplication)).map(Some(_))
              }.getOrElse(DBIO.successful(None))
            }
          }.getOrElse(DBIO.successful(None))
        } yield {
          maybeApplication.map { application =>
            info.maybeBehaviorId.map { behaviorId =>
              Redirect(routes.BehaviorEditorController.edit(behaviorId))
            }.getOrElse {
              Redirect(routes.OAuth2ApplicationController.edit(application.id, Some(application.teamId)))
            }
          }.getOrElse {
            NotFound(s"Team not found: ${info.teamId}")
          }
        }

        dataService.run(action)
      }
    )
  }


}
