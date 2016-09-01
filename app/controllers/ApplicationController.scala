package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import export.{BehaviorVersionImporter, BehaviorVersionZipImporter, BehaviorVersionExporter}
import json._
import json.Formatting._
import models.bots.config.{RequiredOAuth2ApiConfigQueries, AWSConfigQueries}
import models.bots.triggers.MessageTriggerQueries
import models._
import models.accounts._
import models.bots._
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.Action
import play.api.routing.JavaScriptReverseRouter
import services.{GithubService, AWSLambdaService}
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationController @Inject() (
                                        val messagesApi: MessagesApi,
                                        val env: Environment[User, CookieAuthenticator],
                                        val configuration: Configuration,
                                        val models: Models,
                                        val lambdaService: AWSLambdaService,
                                        val testReportBuilder: BehaviorTestReportBuilder,
                                        val ws: WSClient,
                                        val cache: CacheApi,
                                        val socialProviderRegistry: SocialProviderRegistry)
  extends ReAuthable {

  def index(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- user.teamAccessFor(maybeTeamId)
      maybeBehaviors <- teamAccess.maybeTargetTeam.map { team =>
        BehaviorQueries.allForTeam(team).map { behaviors =>
          Some(behaviors)
        }
      }.getOrElse {
        DBIO.successful(None)
      }
      versionData <- DBIO.sequence(maybeBehaviors.map { behaviors =>
        behaviors.map { behavior =>
          BehaviorVersionData.maybeFor(behavior.id, user)
        }
      }.getOrElse(Seq())).map(_.flatten)
      result <- teamAccess.maybeTargetTeam.map { team =>
        DBIO.successful(if (versionData.isEmpty) {
          Redirect(routes.ApplicationController.intro(maybeTeamId))
        } else {
          Ok(views.html.index(teamAccess, versionData))
        })
      }.getOrElse {
        reAuthFor(request, maybeTeamId)
      }
    } yield result

    models.run(action)
  }

  case class PublishedBehaviorInfo(published: Seq[BehaviorCategory], installedBehaviors: Seq[InstalledBehaviorData])

  private def withPublishedBehaviorInfoFor(team: Team): DBIO[PublishedBehaviorInfo] = {
    BehaviorQueries.allForTeam(team).map { behaviors =>
      behaviors.map { ea => InstalledBehaviorData(ea.id, ea.maybeImportedId)}
    }.map { installedBehaviors =>
      val githubService = GithubService(team, ws, configuration, cache)
      PublishedBehaviorInfo(githubService.publishedBehaviorCategories, installedBehaviors)
    }
  }

  def intro(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- user.teamAccessFor(maybeTeamId)
      maybePublishedBehaviorInfo <- teamAccess.maybeTargetTeam.map { team =>
        withPublishedBehaviorInfoFor(team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      result <- (for {
        team <- teamAccess.maybeTargetTeam
        data <- maybePublishedBehaviorInfo
      } yield {
          DBIO.successful(
            Ok(
              views.html.intro(
                teamAccess,
                data.published,
                data.installedBehaviors
              )
            )
          )
        }).getOrElse {
        reAuthFor(request, maybeTeamId)
      }
    } yield result

    models.run(action)
  }

  def installBehaviors(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- user.teamAccessFor(maybeTeamId)
      maybePublishedBehaviorInfo <- teamAccess.maybeTargetTeam.map { team =>
        withPublishedBehaviorInfoFor(team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      result <- (for {
        team <- teamAccess.maybeTargetTeam
        data <- maybePublishedBehaviorInfo
      } yield {
          DBIO.successful(
            Ok(
              views.html.publishedBehaviors(
                teamAccess,
                data.published,
                data.installedBehaviors
              )
            )
          )
        }).getOrElse {
        reAuthFor(request, maybeTeamId)
      }
    } yield result

    models.run(action)
  }

  def newBehavior(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- user.teamAccessFor(maybeTeamId)
      maybeEnvironmentVariables <- teamAccess.maybeTargetTeam.map { team =>
        EnvironmentVariableQueries.allFor(team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeOAuth2Applications <- teamAccess.maybeTargetTeam.map { team =>
        OAuth2ApplicationQueries.allFor(team).map(Some(_))
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

    models.run(action)
  }

  def editBehavior(id: String, maybeJustSaved: Option[Boolean]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeVersionData <- BehaviorVersionData.maybeFor(id, user)
      teamAccess <- user.teamAccessFor(maybeVersionData.map(_.teamId))
      maybeEnvironmentVariables <- teamAccess.maybeTargetTeam.map { team =>
        EnvironmentVariableQueries.allFor(team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeOAuth2Applications <- teamAccess.maybeTargetTeam.map { team =>
        OAuth2ApplicationQueries.allFor(team).map(Some(_))
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

    models.run(action)
  }

  case class SaveBehaviorInfo(
                               dataJson: String,
                               maybeRedirect: Option[String],
                               maybeRequiredOAuth2ApiConfigId: Option[String]
                             )

  private val saveBehaviorForm = Form(
    mapping(
      "dataJson" -> nonEmptyText,
      "redirect" -> optional(nonEmptyText),
      "requiredOAuth2ApiConfigId" -> optional(nonEmptyText)
    )(SaveBehaviorInfo.apply)(SaveBehaviorInfo.unapply)
  )

  def saveBehavior = SecuredAction.async { implicit request =>
    val user = request.identity
    saveBehaviorForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[BehaviorVersionData] match {
          case JsSuccess(data, jsPath) => {
            val action = (for {
              teamAccess <- user.teamAccessFor(Some(data.teamId))
              maybeBehavior <- data.behaviorId.map { behaviorId =>
                BehaviorQueries.find(behaviorId, user)
              }.getOrElse {
                teamAccess.maybeTargetTeam.map { team =>
                  BehaviorQueries.createFor(team, None).map(Some(_))
                }.getOrElse(DBIO.successful(None))
              }
              maybeBehaviorVersion <- maybeBehavior.map { behavior =>
                BehaviorVersionQueries.createFor(behavior, Some(user), lambdaService, data).map(Some(_))
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
                    Redirect(routes.ApplicationController.editBehavior(behavior.id, justSaved = Some(true)))
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

            models.run(action)
          }
          case e: JsError => Future.successful(BadRequest(s"Malformatted data: ${e.toString}"))
        }
      }
    )
  }

  private val deleteBehaviorForm = Form(
    "behaviorId" -> nonEmptyText
  )

  def deleteBehavior = SecuredAction.async { implicit request =>
    deleteBehaviorForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      behaviorId => {
        val action = for {
          maybeBehavior <- BehaviorQueries.find(behaviorId, request.identity)
          _ <- maybeBehavior.map { behavior =>
            behavior.unlearn(lambdaService)
          }.getOrElse(DBIO.successful(Unit))
        } yield Redirect(routes.ApplicationController.index())

        models.run(action)
      }
    )
  }

  def versionInfoFor(behaviorId: String) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeBehavior <- BehaviorQueries.find(behaviorId, user)
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
        AWSConfigQueries.maybeFor(version).map { config =>
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

    models.run(action)
  }

  case class TestBehaviorInfo(behaviorId: String, message: String)

  private val testBehaviorForm = Form(
    mapping(
      "behaviorId" -> nonEmptyText,
      "message" -> nonEmptyText
    )(TestBehaviorInfo.apply)(TestBehaviorInfo.unapply)
  )

  def testBehaviorVersion = SecuredAction.async { implicit request =>
    val user = request.identity
    testBehaviorForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val action = for {
          maybeBehavior <- BehaviorQueries.find(info.behaviorId, user)
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

        models.run(action)
      }
    )
  }

  private val cloneBehaviorForm = Form(
    "behaviorId" -> nonEmptyText
  )

  def cloneBehavior = SecuredAction.async { implicit request =>
    cloneBehaviorForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      behaviorId => {
        val user = request.identity
        val action = for {
          maybeVersionData <- BehaviorVersionData.maybeFor(behaviorId, user)
          maybeTeam <- maybeVersionData.map { data =>
            Team.find(data.teamId, user)
          }.getOrElse(DBIO.successful(None))
          maybeImporter <- DBIO.successful(for {
            team <- maybeTeam
            data <- maybeVersionData
          } yield BehaviorVersionImporter(team, user, lambdaService, data))
          maybeCloned <- maybeImporter.map { importer =>
            importer.run.map(Some(_))
          }.getOrElse(DBIO.successful(None))
        } yield maybeCloned.map { cloned =>
            Redirect(routes.ApplicationController.editBehavior(cloned.behavior.id))
          }.getOrElse {
            NotFound("")
          }

        models.run(action)
      }
    )
  }

  def regexValidationErrorsFor(pattern: String) = SecuredAction { implicit request =>
    val content = MessageTriggerQueries.maybeRegexValidationErrorFor(pattern).map { errMessage =>
      Array(errMessage)
    }.getOrElse {
      Array()
    }
    Ok(Json.toJson(Array(content)))
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.ApplicationController.regexValidationErrorsFor,
        routes.javascript.ApplicationController.versionInfoFor
      )
    ).as("text/javascript")
  }

}
