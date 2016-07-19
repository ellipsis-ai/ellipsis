package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import export.{BehaviorVersionImporter, BehaviorVersionZipImporter, BehaviorVersionExporter}
import json._
import json.Formatting._
import models.bots.config.AWSConfigQueries
import models.bots.triggers.MessageTriggerQueries
import models._
import models.accounts.User
import models.bots._
import models.UINotificationFormatting._
import play.api.Configuration
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
                                        socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] {

  def index(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeTeam <- user.maybeTeamFor(maybeTeamId)
      maybeBehaviors <- maybeTeam.map { team =>
        BehaviorQueries.allForTeam(team).map { behaviors =>
          Some(behaviors)
        }
      }.getOrElse {
        DBIO.successful(None)
      }
      maybeVersionData <- DBIO.sequence(maybeBehaviors.map { behaviors =>
        behaviors.map { behavior =>
          BehaviorVersionData.maybeFor(behavior.id, user)
        }
      }.getOrElse(Seq()))
    } yield {
      maybeTeam.map { team =>
        Ok(views.html.index(Some(user), team, maybeVersionData.flatten))
      }.getOrElse(NotFound(s"No accessible team"))
    }

    models.run(action)
  }

  def publishedBehaviors(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeTeam <- user.maybeTeamFor(maybeTeamId)
      maybeGithubService <- DBIO.successful(maybeTeam.map { team =>
        GithubService(team, ws, configuration)
      })
      installedBehaviors <- maybeTeam.map { team =>
        BehaviorQueries.allForTeam(team).map { behaviors =>
          behaviors.map { ea => InstalledBehaviorData(ea.id, ea.maybeImportedId)}
        }
      }.getOrElse(DBIO.successful(Seq()))
      data <- maybeGithubService.map { service =>
        DBIO.from(service.fetchPublishedBehaviorCategories)
      }.getOrElse(DBIO.successful(Seq()))
    } yield {
        maybeTeam.map { team =>
          Ok(views.html.publishedBehaviors(Some(user), team, data, installedBehaviors))
        }.getOrElse {
          NotFound(s"No accessible team")
        }
      }

    models.run(action)
  }

  def newBehavior(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeTeam <- user.maybeTeamFor(maybeTeamId)
      maybeEnvironmentVariables <- maybeTeam.map { team =>
        EnvironmentVariableQueries.allFor(team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
        (for {
          team <- maybeTeam
          envVars <- maybeEnvironmentVariables
        } yield {
            val data = BehaviorVersionData(
              team.id,
              None,
              "",
              "",
              Seq(),
              Seq(),
              BehaviorConfig(None, None),
              None
            )
            Ok(views.html.edit(
              Some(user),
              Json.toJson(data).toString,
              Json.toJson(envVars.map(EnvironmentVariableData.withoutValueFor)).toString,
              justSaved = false,
              notificationsJson = Json.toJson(Array[String]()).toString
            ))
          }).getOrElse {
          // TODO: platform-agnostic
          Redirect(routes.SlackController.signIn(Some(request.uri)))
        }
      }

    models.run(action)
  }

  def editBehavior(id: String, maybeJustSaved: Option[Boolean]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeVersionData <- BehaviorVersionData.maybeFor(id, user)
      maybeBehavior <- BehaviorQueries.find(id, user)
      maybeBehaviorVersion <- maybeBehavior.map { behavior =>
        behavior.maybeCurrentVersion
      }.getOrElse(DBIO.successful(None))
      maybeEnvironmentVariables <- maybeBehaviorVersion.map { behaviorVersion =>
        EnvironmentVariableQueries.allFor(behaviorVersion.team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
        (for {
          data <- maybeVersionData
          envVars <- maybeEnvironmentVariables
        } yield {
          Ok(views.html.edit(
            Some(user),
            Json.toJson(data).toString,
            Json.toJson(envVars.map(EnvironmentVariableData.withoutValueFor)).toString,
            maybeJustSaved.exists(identity),
            notificationsJson = Json.toJson(Array[String]()).toString
          ))
        }).getOrElse {
          NotFound(views.html.notFound(Some(user), Some("Behavior not found"), Some("The behavior you are trying to access could not be found.")))
        }
    }

    models.run(action)
  }

  case class SaveBehaviorInfo(dataJson: String)

  private val saveBehaviorForm = Form(
    mapping(
      "dataJson" -> nonEmptyText
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
              maybeTeam <- Team.find(data.teamId, user)
              maybeBehavior <- data.behaviorId.map { behaviorId =>
                BehaviorQueries.find(behaviorId, user)
              }.getOrElse {
                maybeTeam.map { team =>
                  BehaviorQueries.createFor(team, None).map(Some(_))
                }.getOrElse(DBIO.successful(None))
              }
              maybeBehaviorVersion <- maybeBehavior.map { behavior =>
                BehaviorVersionQueries.createFor(behavior, lambdaService, data).map(Some(_))
              }.getOrElse(DBIO.successful(None))
            } yield {
                maybeBehavior.map { behavior =>
                  Redirect(routes.ApplicationController.editBehavior(behavior.id, justSaved = Some(true)))
                }.getOrElse {
                  NotFound(views.html.notFound(Some(user), Some("Behavior not found"), Some("The behavior you were trying to save could not be found.")))
                }
              }) transactionally

            models.run(action)
          }
          case e: JsError => Future.successful(BadRequest("Malformatted data"))
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
    } yield {
        maybeBehavior.map { behavior =>
          val versionsData = versions.map { version =>
            val maybeAwsConfigData = awsConfigByVersion.get(version).flatMap { maybeConfig =>
              maybeConfig.map { config =>
                AWSConfigData(config.maybeAccessKeyName, config.maybeSecretKeyName, config.maybeRegionName)
              }
            }
            BehaviorVersionData(
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
              BehaviorConfig(None, maybeAwsConfigData),
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

  def exportBehavior(id: String) = SecuredAction.async { implicit request =>
    val action = BehaviorVersionExporter.maybeFor(id, request.identity).map { maybeExporter =>
      maybeExporter.map { exporter =>
        Ok.sendFile(exporter.getZipFile)
      }.getOrElse {
        NotFound(s"Behavior not found: $id")
      }
    }

    models.run(action)
  }

  def importBehaviorZip(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = user.maybeTeamFor(maybeTeamId).map { maybeTeam =>
      maybeTeam.map { team =>
        Ok(views.html.importBehaviorZip(Some(user), team))
      }.getOrElse {
        NotFound(s"No accessible team")
      }
    }

    models.run(action)
  }

  case class ImportBehaviorZipInfo(teamId: String)

  private val importBehaviorZipForm = Form(
    mapping(
      "teamId" -> nonEmptyText
    )(ImportBehaviorZipInfo.apply)(ImportBehaviorZipInfo.unapply)
  )

  def doImportBehaviorZip = SecuredAction.async { implicit request =>
    (for {
      formData <- request.body.asMultipartFormData
      zipFile <- formData.file("zipFile")
    } yield {
      importBehaviorZipForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(formWithErrors.errorsAsJson))
        },
        info => {
          val action = for {
            maybeTeam <- Team.find(info.teamId, request.identity)
            maybeImporter <- DBIO.successful(maybeTeam.map { team =>
              BehaviorVersionZipImporter(team, lambdaService, zipFile.ref.file)
            })
            maybeBehaviorVersion <- maybeImporter.map { importer =>
              importer.run.map(Some(_))
            }.getOrElse(DBIO.successful(None))
          } yield {
              maybeBehaviorVersion.map { behaviorVersion =>
                Redirect(routes.ApplicationController.editBehavior(behaviorVersion.behavior.id))
              }.getOrElse {
                NotFound(s"Team not found: ${info.teamId}")
              }
            }

          models.run(action)
        }
      )
    }).getOrElse(Future.successful(BadRequest("")))

  }

  case class ImportBehaviorInfo(teamId: String, dataJson: String)

  private val importBehaviorForm = Form(
    mapping(
      "teamId" -> nonEmptyText,
      "dataJson" -> nonEmptyText
    )(ImportBehaviorInfo.apply)(ImportBehaviorInfo.unapply)
  )

  def doImportBehavior = SecuredAction.async { implicit request =>
    val user = request.identity
    importBehaviorForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[BehaviorVersionData] match {
          case JsSuccess(data, jsPath) => {
            val action = for {
              maybeTeam <- Team.find(data.teamId, user)
              maybeImporter <- DBIO.successful(maybeTeam.map { team =>
                BehaviorVersionImporter(team, lambdaService, data)
              })
              maybeBehaviorVersion <- maybeImporter.map { importer =>
                importer.run.map(Some(_))
              }.getOrElse(DBIO.successful(None))
            } yield {
                maybeBehaviorVersion.map { behaviorVersion =>
                  if (request.headers.get("x-requested-with").contains("XMLHttpRequest")) {
                    Ok(Json.obj("behaviorId" -> behaviorVersion.behavior.id))
                  } else {
                    Redirect(routes.ApplicationController.editBehavior(behaviorVersion.behavior.id, justSaved = Some(true)))
                  }
                }.getOrElse {
                  NotFound("Behavior not found")
                }
              }

            models.run(action)
          }
          case e: JsError => Future.successful(BadRequest("Malformatted data"))
        }
      }
    )
  }

  case class EnvironmentVariablesInfo(teamId: String, dataJson: String)

  private val submitEnvironmentVariablesForm = Form(
    mapping(
      "teamId" -> nonEmptyText,
      "dataJson" -> nonEmptyText
    )(EnvironmentVariablesInfo.apply)(EnvironmentVariablesInfo.unapply)
  )

  def submitEnvironmentVariables = SecuredAction.async { implicit request =>
    val user = request.identity
    submitEnvironmentVariablesForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[EnvironmentVariablesData] match {
          case JsSuccess(data, jsPath) => {
            val action = (for {
              maybeTeam <- Team.find(data.teamId, user)
              maybeEnvironmentVariables <- maybeTeam.map { team =>
                DBIO.sequence(data.variables.map { envVarData =>
                  EnvironmentVariableQueries.ensureFor(envVarData.name, envVarData.value, team)
                }).map( vars => Some(vars.flatten) )
              }.getOrElse(DBIO.successful(None))
            } yield {
              maybeEnvironmentVariables.map { envVars =>
                Ok(
                  Json.toJson(
                    EnvironmentVariablesData(
                      data.teamId,
                      envVars.map( ea => EnvironmentVariableData.withoutValueFor(ea) )
                    )
                  )
                )
              }.getOrElse {
                NotFound(s"Team not found: ${data.teamId}")
              }
            }) transactionally

            models.run(action)
          }
          case e: JsError => Future.successful(BadRequest("Malformatted data"))
        }
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
        routes.javascript.ApplicationController.versionInfoFor,
        routes.javascript.ApplicationController.submitEnvironmentVariables
      )
    ).as("text/javascript")
  }

}
