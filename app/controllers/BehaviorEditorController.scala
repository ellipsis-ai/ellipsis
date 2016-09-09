package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import export.BehaviorVersionImporter
import json._
import json.Formatting._
import models.bots.config.{AWSConfigQueries, RequiredOAuth2ApiConfigQueries}
import models.bots.triggers.MessageTriggerQueries
import models.accounts._
import models.bots._
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import services.{AWSLambdaService, DataService}
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorEditorController @Inject() (
                                           val messagesApi: MessagesApi,
                                           val silhouette: Silhouette[EllipsisEnv],
                                           val configuration: Configuration,
                                           val dataService: DataService,
                                           val lambdaService: AWSLambdaService,
                                           val testReportBuilder: BehaviorTestReportBuilder
                                         ) extends ReAuthable {

  def newBehavior(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- DBIO.from(dataService.users.teamAccessFor(user, maybeTeamId))
      maybeEnvironmentVariables <- teamAccess.maybeTargetTeam.map { team =>
        DBIO.from(dataService.environmentVariables.allFor(team).map(Some(_)))
      }.getOrElse(DBIO.successful(None))
      maybeOAuth2Applications <- teamAccess.maybeTargetTeam.map { team =>
        DBIO.from(dataService.oauth2Applications.allFor(team)).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      oauth2Apis <- OAuth2ApiQueries.allFor(teamAccess.maybeTargetTeam)
      result <- (for {
        team <- teamAccess.maybeTargetTeam
        envVars <- maybeEnvironmentVariables
        oauth2Applications <- maybeOAuth2Applications
      } yield {
        val data = BehaviorVersionData.buildFor(
          team.id,
          None,
          "",
          "",
          Seq(),
          Seq(),
          BehaviorConfig(None, None, None),
          None,
          None,
          None
        )
        DBIO.successful(Ok(views.html.edit(
          teamAccess,
          Json.toJson(data).toString,
          Json.toJson(envVars.map(EnvironmentVariableData.withoutValueFor)).toString,
          Json.toJson(oauth2Applications.map(OAuth2ApplicationData.from)).toString,
          Json.toJson(oauth2Apis.map(OAuth2ApiData.from)).toString,
          justSaved = false,
          notificationsJson = Json.toJson(Array[String]()).toString
        )))
      }).getOrElse {
        reAuthFor(request, maybeTeamId)
      }
    } yield result

    dataService.run(action)
  }

  def edit(id: String, maybeJustSaved: Option[Boolean]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeVersionData <- BehaviorVersionData.maybeFor(id, user, dataService)
      teamAccess <- DBIO.from(dataService.users.teamAccessFor(user, maybeVersionData.map(_.teamId)))
      maybeEnvironmentVariables <- teamAccess.maybeTargetTeam.map { team =>
        DBIO.from(dataService.environmentVariables.allFor(team).map(Some(_)))
      }.getOrElse(DBIO.successful(None))
      maybeOAuth2Applications <- teamAccess.maybeTargetTeam.map { team =>
        DBIO.from(dataService.oauth2Applications.allFor(team)).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      oauth2Apis <- OAuth2ApiQueries.allFor(teamAccess.maybeTargetTeam)
      result <- (for {
        data <- maybeVersionData
        envVars <- maybeEnvironmentVariables
        oauth2Applications <- maybeOAuth2Applications
      } yield {
        DBIO.successful(Ok(views.html.edit(
          teamAccess,
          Json.toJson(data).toString,
          Json.toJson(envVars.map(EnvironmentVariableData.withoutValueFor)).toString,
          Json.toJson(oauth2Applications.map(OAuth2ApplicationData.from)).toString,
          Json.toJson(oauth2Apis.map(OAuth2ApiData.from)).toString,
          maybeJustSaved.exists(identity),
          notificationsJson = Json.toJson(Array[String]()).toString
        )))
      }).getOrElse {
        val response = NotFound(
          views.html.notFound(
            Some(teamAccess),
            Some("Behavior not found"),
            Some("The behavior you are trying to access could not be found."),
            Some(reAuthLinkFor(request, None))
          ))
        withAuthDiscarded(request, response)
      }
    } yield result

    dataService.run(action)
  }

  case class SaveBehaviorInfo(
                               dataJson: String,
                               maybeRedirect: Option[String],
                               maybeRequiredOAuth2ApiConfigId: Option[String]
                             )

  private val saveForm = Form(
    mapping(
      "dataJson" -> nonEmptyText,
      "redirect" -> optional(nonEmptyText),
      "requiredOAuth2ApiConfigId" -> optional(nonEmptyText)
    )(SaveBehaviorInfo.apply)(SaveBehaviorInfo.unapply)
  )

  def save = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    saveForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[BehaviorVersionData] match {
          case JsSuccess(data, jsPath) => {
            val action = (for {
              teamAccess <- DBIO.from(dataService.users.teamAccessFor(user, Some(data.teamId)))
              maybeBehavior <- data.behaviorId.map { behaviorId =>
                BehaviorQueries.find(behaviorId, user, dataService)
              }.getOrElse {
                teamAccess.maybeTargetTeam.map { team =>
                  BehaviorQueries.createFor(team, None).map(Some(_))
                }.getOrElse(DBIO.successful(None))
              }
              maybeBehaviorVersion <- maybeBehavior.map { behavior =>
                BehaviorVersionQueries.createFor(behavior, Some(user), lambdaService, data, dataService).map(Some(_))
              }.getOrElse(DBIO.successful(None))
              maybePreviousRequiredOAuth2ApiConfig <- info.maybeRequiredOAuth2ApiConfigId.map { id =>
                RequiredOAuth2ApiConfigQueries.find(id)
              }.getOrElse(DBIO.successful(None))
              maybeRequiredOAuth2ApiConfig <- maybePreviousRequiredOAuth2ApiConfig.flatMap { config =>
                maybeBehaviorVersion.map { version =>
                  RequiredOAuth2ApiConfigQueries.allFor(config.api, version).map(_.headOption)
                }
              }.getOrElse(DBIO.successful(None))
            } yield {
              maybeBehavior.map { behavior =>
                if (info.maybeRedirect.contains("newOAuth2Application")) {
                  Redirect(routes.OAuth2ApplicationController.newApp(maybeRequiredOAuth2ApiConfig.map(_.id), Some(data.teamId), Some(behavior.id)))
                } else {
                  Redirect(routes.BehaviorEditorController.edit(behavior.id, justSaved = Some(true)))
                }
              }.getOrElse {
                NotFound(
                  views.html.notFound(
                    Some(teamAccess),
                    Some("Behavior not found"),
                    Some("The behavior you were trying to save could not be found."
                    )
                  )
                )
              }
            }) transactionally

            dataService.run(action)
          }
          case e: JsError => Future.successful(BadRequest(s"Malformatted data: ${e.toString}"))
        }
      }
    )
  }

  private val deleteForm = Form(
    "behaviorId" -> nonEmptyText
  )

  def delete = silhouette.SecuredAction.async { implicit request =>
    deleteForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      behaviorId => {
        val action = for {
          maybeBehavior <- BehaviorQueries.find(behaviorId, request.identity, dataService)
          _ <- maybeBehavior.map { behavior =>
            behavior.unlearn(lambdaService)
          }.getOrElse(DBIO.successful(Unit))
        } yield Redirect(routes.ApplicationController.index())

        dataService.run(action)
      }
    )
  }

  def versionInfoFor(behaviorId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeBehavior <- BehaviorQueries.find(behaviorId, user, dataService)
      versions <- maybeBehavior.map { behavior =>
        BehaviorVersionQueries.allFor(behavior)
      }.getOrElse(DBIO.successful(Seq()))
      parametersByVersion <- DBIO.sequence(versions.map { version =>
        BehaviorParameterQueries.allFor(version).map { params =>
          (version, params)
        }
      }).map(_.toMap)
      triggersByVersion <- DBIO.sequence(versions.map { version =>
        MessageTriggerQueries.allFor(version).map { triggers =>
          (version, triggers)
        }
      }).map(_.toMap)
      awsConfigByVersion <- DBIO.sequence(versions.map { version =>
        AWSConfigQueries.maybeFor(version, dataService).map { config =>
          (version, config)
        }
      }).map(_.toMap)
      requiredOAuth2ApiConfigsByVersion <- DBIO.sequence(versions.map { version =>
        RequiredOAuth2ApiConfigQueries.allFor(version).map { apps =>
          (version, apps)
        }
      }).map(_.toMap)
    } yield {
      maybeBehavior.map { behavior =>
        val versionsData = versions.map { version =>
          val maybeAwsConfigData = awsConfigByVersion.get(version).flatMap { maybeConfig =>
            maybeConfig.map { config =>
              AWSConfigData(config.maybeAccessKeyName, config.maybeSecretKeyName, config.maybeRegionName)
            }
          }
          val maybeRequiredOAuth2ApiConfigsData = requiredOAuth2ApiConfigsByVersion.get(version).map { configs =>
            configs.map(ea => RequiredOAuth2ApiConfigData.from(ea))
          }
          BehaviorVersionData.buildFor(
            version.team.id,
            Some(behavior.id),
            version.functionBody,
            version.maybeResponseTemplate.getOrElse(""),
            parametersByVersion.get(version).map { params =>
              params.map { ea =>
                BehaviorParameterData(ea.name, ea.question)
              }
            }.getOrElse(Seq()),
            triggersByVersion.get(version).map { triggers =>
              triggers.map { ea =>
                BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
              }
            }.getOrElse(Seq()),
            BehaviorConfig(None, maybeAwsConfigData, maybeRequiredOAuth2ApiConfigsData),
            behavior.maybeImportedId,
            None,
            Some(version.createdAt)
          )
        }
        Ok(Json.toJson(versionsData))
      }.getOrElse {
        NotFound(Json.toJson("Error: behavior not found"))
      }
    }

    dataService.run(action)
  }

  case class TestBehaviorInfo(behaviorId: String, message: String)

  private val testForm = Form(
    mapping(
      "behaviorId" -> nonEmptyText,
      "message" -> nonEmptyText
    )(TestBehaviorInfo.apply)(TestBehaviorInfo.unapply)
  )

  def test = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    testForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val action = for {
          maybeBehavior <- BehaviorQueries.find(info.behaviorId, user, dataService)
          maybeBehaviorVersion <- maybeBehavior.map { behavior =>
            behavior.maybeCurrentVersion
          }.getOrElse(DBIO.successful(None))
          maybeReport <- maybeBehaviorVersion.map { behaviorVersion =>
            val context = TestMessageContext(info.message, includesBotMention = true)
            testReportBuilder.buildFor(TestEvent(context), behaviorVersion).map(Some(_))
          }.getOrElse(DBIO.successful(None))

        } yield {
          maybeReport.map { report =>
            Ok(report.json)
          }.getOrElse {
            NotFound(s"Behavior not found: ${info.behaviorId}")
          }
        }

        dataService.run(action)
      }
    )
  }

  private val cloneForm = Form(
    "behaviorId" -> nonEmptyText
  )

  def duplicate = silhouette.SecuredAction.async { implicit request =>
    cloneForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      behaviorId => {
        val user = request.identity
        val action = for {
          maybeVersionData <- BehaviorVersionData.maybeFor(behaviorId, user, dataService)
          maybeTeam <- maybeVersionData.map { data =>
            DBIO.from(dataService.teams.find(data.teamId, user))
          }.getOrElse(DBIO.successful(None))
          maybeImporter <- DBIO.successful(for {
            team <- maybeTeam
            data <- maybeVersionData
          } yield BehaviorVersionImporter(team, user, lambdaService, data, dataService))
          maybeCloned <- maybeImporter.map { importer =>
            importer.run.map(Some(_))
          }.getOrElse(DBIO.successful(None))
        } yield maybeCloned.map { cloned =>
          Redirect(routes.BehaviorEditorController.edit(cloned.behavior.id))
        }.getOrElse {
          NotFound("")
        }

        dataService.run(action)
      }
    )
  }

  def regexValidationErrorsFor(pattern: String) = silhouette.SecuredAction { implicit request =>
    val content = MessageTriggerQueries.maybeRegexValidationErrorFor(pattern).map { errMessage =>
      Array(errMessage)
    }.getOrElse {
      Array()
    }
    Ok(Json.toJson(Array(content)))
  }

}
