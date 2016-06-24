package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import json.EditorFormat._
import json.ExportFormat._
import models.bots.triggers.MessageTriggerQueries
import models.{EnvironmentVariable, Team, EnvironmentVariableQueries, Models}
import models.accounts.User
import models.bots._
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.mvc.AnyContent
import services.AWSLambdaService
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
                                        socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] {

  def index = SecuredAction { implicit request => Ok(views.html.index()) }

  def newBehavior(teamId: String) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeTeam <- Team.find(teamId, user)
      maybeEnvironmentVariables <- maybeTeam.map { team =>
        EnvironmentVariableQueries.allFor(team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
        (for {
          team <- maybeTeam
          envVars <- maybeEnvironmentVariables
        } yield {
            val data = SaveBehaviorVersionData(
              team.id,
              None,
              "",
              "",
              Seq(),
              Seq(),
              None
            )
            Ok(views.html.edit(Json.toJson(data).toString, envVars.map(_.name), justSaved = false))
          }).getOrElse {
          // TODO: platform-agnostic
          Redirect(routes.SlackController.signIn(Some(request.uri)))
        }
      }

    models.run(action)
  }

  private def maybeVersionDataFor(
                                   behaviorId: String
                                   )(implicit request: SecuredRequest[AnyContent]
    ): DBIO[Option[(SaveBehaviorVersionData, Seq[EnvironmentVariable])]] = {

    val user = request.identity
    for {
      maybeBehavior <- BehaviorQueries.find(behaviorId, user)
      maybeBehaviorVersion <- maybeBehavior.map { behavior =>
        behavior.maybeCurrentVersion
      }.getOrElse(DBIO.successful(None))
      maybeParameters <- maybeBehaviorVersion.map { behaviorVersion =>
        BehaviorParameterQueries.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeTriggers <- maybeBehaviorVersion.map { behaviorVersion =>
        MessageTriggerQueries.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeEnvironmentVariables <- maybeBehaviorVersion.map { behaviorVersion =>
        EnvironmentVariableQueries.allFor(behaviorVersion.team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
      for {
        behavior <- maybeBehavior
        behaviorVersion <- maybeBehaviorVersion
        params <- maybeParameters
        triggers <- maybeTriggers
        envVars <- maybeEnvironmentVariables
      } yield {
        val data = SaveBehaviorVersionData(
          behaviorVersion.team.id,
          Some(behavior.id),
          behaviorVersion.functionBody,
          behaviorVersion.maybeResponseTemplate.getOrElse(""),
          params.map { ea =>
            BehaviorParameterData(ea.name, ea.question)
          },
          triggers.map( ea =>
            BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
          ),
          Some(behaviorVersion.createdAt)
        )
        (data, envVars)
      }
    }
  }

  def editBehavior(id: String, maybeJustSaved: Option[Boolean]) = SecuredAction.async { implicit request =>
    val action = maybeVersionDataFor(id).map { maybeTuple =>
      maybeTuple.map { case(data, envVars) =>
        Ok(views.html.edit(Json.toJson(data).toString, envVars.map(_.name), maybeJustSaved.exists(identity)))
      }.getOrElse {
        NotFound("Behavior not found")
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
        json.validate[SaveBehaviorVersionData] match {
          case JsSuccess(data, jsPath) => {
            val action = (for {
              maybeTeam <- Team.find(data.teamId, user)
              maybeBehavior <- data.behaviorId.map { behaviorId =>
                BehaviorQueries.find(behaviorId, user)
              }.getOrElse {
                maybeTeam.map { team =>
                  BehaviorQueries.createFor(team).map(Some(_))
                }.getOrElse(DBIO.successful(None))
              }
              maybeBehaviorVersion <- maybeBehavior.map { behavior =>
                BehaviorVersionQueries.createFor(behavior, lambdaService, data).map(Some(_))
              }.getOrElse(DBIO.successful(None))
            } yield {
                maybeBehavior.map { behavior =>
                  Redirect(routes.ApplicationController.editBehavior(behavior.id, justSaved = Some(true)))
                }.getOrElse {
                  NotFound("Behavior not found")
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
    } yield {
        maybeBehavior.map { behavior =>
          val versionsData = versions.map { version =>
            SaveBehaviorVersionData(
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
              Some(version.createdAt)
            )
          }
          Ok(Json.toJson(versionsData))
        }.getOrElse {
          NotFound("Behavior not found")
        }
      }

    models.run(action)
  }

  private val restoreToVersionForm = Form(
    "behaviorVersionId" -> nonEmptyText
  )

  def restoreToVersion = SecuredAction.async { implicit request =>
    val user = request.identity
    restoreToVersionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      behaviorVersionId => {
        val action = for {
          maybeBehaviorVersion <- BehaviorVersionQueries.find(behaviorVersionId, user)
          _ <- maybeBehaviorVersion.map { behaviorVersion =>
            behaviorVersion.restore
          }.getOrElse(DBIO.successful(Unit))
        } yield {
          maybeBehaviorVersion.map { behaviorVersion =>
            Redirect(routes.ApplicationController.editBehavior(behaviorVersion.behavior.id))
          }.getOrElse {
            NotFound(s"Behavior version not found: $behaviorVersionId")
          }
        }

        models.run(action)
      }
    )
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
    val action = maybeVersionDataFor(id).map { maybeTuple =>
      maybeTuple.map { case(data, _) =>
        Ok(Json.prettyPrint(Json.toJson(data.forExport)))
      }.getOrElse {
        NotFound("Behavior not found")
      }
    }

    models.run(action)
  }

  def importBehavior(teamId: String) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = Team.find(teamId, user).map { maybeTeam =>
      maybeTeam.map { team =>
        Ok(views.html.importBehavior(team))
      }.getOrElse {
        NotFound(s"Team not found $teamId")
      }
    }

    models.run(action)
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
        json.validate[ExportBehaviorVersionData] match {
          case JsSuccess(data, jsPath) => {
            val action = for {
              maybeTeam <- Team.find(info.teamId, user)
              maybeBehavior <- maybeTeam.map { team =>
                BehaviorQueries.createFor(team).map(Some(_))
              }.getOrElse(DBIO.successful(None))
              maybeBehaviorVersion <- maybeBehavior.map { behavior =>
                BehaviorVersionQueries.createFor(behavior, lambdaService, data).map(Some(_))
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

}
