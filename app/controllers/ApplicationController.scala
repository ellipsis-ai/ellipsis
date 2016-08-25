package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import export.{BehaviorVersionImporter, BehaviorVersionZipImporter, BehaviorVersionExporter}
import json._
import json.Formatting._
import models.bots.config.{RequiredOAuth2ApplicationQueries, AWSConfigQueries}
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
            } yield {
                maybeBehavior.map { behavior =>
                  Redirect(routes.ApplicationController.editBehavior(behavior.id, justSaved = Some(true)))
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
      requiredOAuth2ApplicationsByVersion <- DBIO.sequence(versions.map { version =>
        RequiredOAuth2ApplicationQueries.allFor(version).map { apps =>
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
            val maybeRequiredOAuth2ApplicationsData = requiredOAuth2ApplicationsByVersion.get(version).map { apps =>
              apps.map(ea => OAuth2ApplicationData.from(ea.application))
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
              BehaviorConfig(None, maybeAwsConfigData, maybeRequiredOAuth2ApplicationsData),
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

  def importBehaviorZip(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = user.teamAccessFor(maybeTeamId).map { teamAccess =>
      teamAccess.maybeTargetTeam.map { team =>
        Ok(views.html.importBehaviorZip(teamAccess))
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
              BehaviorVersionZipImporter(team, request.identity, lambdaService, zipFile.ref.file)
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
                BehaviorVersionImporter(team, user, lambdaService, data)
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

  def listOAuth2Applications(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- user.teamAccessFor(maybeTeamId)
      apis <- OAuth2ApiQueries.allFor(teamAccess.maybeTargetTeam)
      applications <- teamAccess.maybeTargetTeam.map { team =>
        OAuth2ApplicationQueries.allFor(team)
      }.getOrElse(DBIO.successful(Seq()))
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
    models.run(action)
  }

  def newOAuth2Application(maybeApiId: Option[String], maybeTeamId: Option[String], maybeBehaviorId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- user.teamAccessFor(maybeTeamId)
      apis <- OAuth2ApiQueries.allFor(teamAccess.maybeTargetTeam)
    } yield {
      teamAccess.maybeTargetTeam.map { team =>
        Ok(views.html.newOAuth2Application(teamAccess, apis.map(api => OAuth2ApiData.from(api)), IDs.next, maybeApiId, maybeBehaviorId))
      }.getOrElse {
        NotFound("Team not accessible")
      }
    }

    models.run(action)
  }

  def editOAuth2Application(id: String, maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- user.teamAccessFor(maybeTeamId)
      apis <- OAuth2ApiQueries.allFor(teamAccess.maybeTargetTeam)
      maybeApplication <- teamAccess.maybeTargetTeam.map { team =>
        OAuth2ApplicationQueries.find(id)
      }.getOrElse(DBIO.successful(None))
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

    models.run(action)
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

  private val saveOAuth2ApplicationForm = Form(
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

  def saveOAuth2Application = SecuredAction.async { implicit request =>
    val user = request.identity
    saveOAuth2ApplicationForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val action = for {
          maybeTeam <- Team.find(info.teamId, user)
          maybeApi <- OAuth2ApiQueries.find(info.apiId)
          maybeApplication <- (for {
            api <- maybeApi
            team <- maybeTeam
          } yield {
              val instance = OAuth2Application(info.id, info.name, api, info.clientId, info.clientSecret, info.maybeScope, info.teamId)
              OAuth2ApplicationQueries.save(instance).map(Some(_))
            }).getOrElse(DBIO.successful(None))
          maybeBehaviorVersion <- info.maybeBehaviorId.map { behaviorId =>
            BehaviorQueries.find(behaviorId, user).flatMap { maybeBehavior =>
              maybeBehavior.map { behavior =>
                behavior.maybeCurrentVersion
              }.getOrElse(DBIO.successful(None))
            }
          }.getOrElse(DBIO.successful(None))
          maybeRequired <- maybeApplication.flatMap { application =>
            maybeBehaviorVersion.map { behaviorVersion =>
              RequiredOAuth2ApplicationQueries.createFor(application, behaviorVersion).map(Some(_))
            }
          }.getOrElse(DBIO.successful(None))
        } yield {
            maybeApplication.map { application =>
              info.maybeBehaviorId.map { behaviorId =>
                Redirect(routes.ApplicationController.editBehavior(behaviorId))
              }.getOrElse {
                Redirect(routes.ApplicationController.editOAuth2Application(application.id, Some(application.teamId)))
              }
            }.getOrElse {
              NotFound(s"Team not found: ${info.teamId}")
            }
          }

        models.run(action)
      }
    )
  }

  def newOAuth2Api(maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = user.teamAccessFor(maybeTeamId).map { teamAccess =>
      teamAccess.maybeTargetTeam.map { _ =>
        Ok(views.html.oAuth2Api(teamAccess, None))
      }.getOrElse {
        NotFound("Team not accessible")
      }
    }

    models.run(action)
  }

  def editOAuth2Api(apiId: String, maybeTeamId: Option[String]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      teamAccess <- user.teamAccessFor(maybeTeamId)
      maybeApi <- OAuth2ApiQueries.find(apiId)
    } yield {
        teamAccess.maybeTargetTeam.map { team =>
          Ok(views.html.oAuth2Api(teamAccess, maybeApi))
        }.getOrElse {
          NotFound("Team not accessible")
        }
      }

    models.run(action)
  }

  case class OAuth2ApiInfo(
                            maybeId: Option[String],
                            name: String,
                            authorizationUrl: String,
                            accessTokenUrl: String,
                            maybeNewApplicationUrl: Option[String],
                            maybeScopeDocumentationUrl: Option[String],
                            maybeTeamId: Option[String]
                            )


  private val saveOAuth2ApiForm = Form(
    mapping(
      "id" -> optional(nonEmptyText),
      "name" -> nonEmptyText,
      "authorizationUrl" -> nonEmptyText,
      "accessTokenUrl" -> nonEmptyText,
      "newApplicationUrl" -> optional(nonEmptyText),
      "scopeDocumentationUrl" -> optional(nonEmptyText),
      "teamId" -> optional(nonEmptyText)
    )(OAuth2ApiInfo.apply)(OAuth2ApiInfo.unapply)
  )

  def saveOAuth2Api = SecuredAction.async { implicit request =>
    val user = request.identity
    saveOAuth2ApiForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val action = for {
          maybeExistingApi <- info.maybeId.map { id =>
            OAuth2ApiQueries.find(id)
          }.getOrElse(DBIO.successful(None))
          api <- OAuth2ApiQueries.save(maybeExistingApi.map { existing =>
            existing.copy(
              name = info.name,
              authorizationUrl = info.authorizationUrl,
              accessTokenUrl = info.accessTokenUrl,
              maybeNewApplicationUrl = info.maybeNewApplicationUrl,
              maybeScopeDocumentationUrl = info.maybeScopeDocumentationUrl
            )
          }.getOrElse {
            OAuth2Api(
              IDs.next,
              info.name,
              info.authorizationUrl,
              info.accessTokenUrl,
              info.maybeNewApplicationUrl,
              info.maybeScopeDocumentationUrl,
              None
            )
          })
        } yield {
            Redirect(routes.ApplicationController.editOAuth2Api(api.id, None))
          }

        models.run(action)
      }
    )
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
