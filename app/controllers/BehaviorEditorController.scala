package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import export.BehaviorVersionImporter
import json._
import json.Formatting._
import models.behaviors.BehaviorResponse
import models.behaviors.testing.{InvocationTester, TestEvent, TestMessageContext, TriggerTester}
import models.behaviors.triggers.messagetrigger.MessageTrigger
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import services.{AWSLambdaConstants, AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorEditorController @Inject() (
                                           val messagesApi: MessagesApi,
                                           val silhouette: Silhouette[EllipsisEnv],
                                           val configuration: Configuration,
                                           val dataService: DataService,
                                           val lambdaService: AWSLambdaService,
                                           val cache: CacheApi
                                         ) extends ReAuthable {

  private def newBehavior(isForDataType: Boolean, maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    BehaviorEditorData.buildForNew(user, maybeTeamId, isForDataType, dataService).flatMap { maybeEditorData =>
      maybeEditorData.map { editorData =>
        Future.successful(Ok(views.html.editBehavior(editorData)))
      }.getOrElse {
        dataService.users.teamAccessFor(user, None).flatMap { teamAccess =>
          val response = NotFound(
            views.html.notFound(
              Some(teamAccess),
              Some("Behavior not found"),
              Some("The behavior you are trying to access could not be found."),
              Some(reAuthLinkFor(request, None))
            ))

          withAuthDiscarded(request, response)
        }
      }
    }
  }

  def newForNormalBehavior(maybeTeamId: Option[String]) = newBehavior(isForDataType = false, maybeTeamId)

  def newForDataType(maybeTeamId: Option[String]) = newBehavior(isForDataType = true, maybeTeamId)

  def edit(id: String, maybeJustSaved: Option[Boolean]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    BehaviorEditorData.buildForEdit(user, id, maybeJustSaved, dataService).flatMap { maybeEditorData =>
      maybeEditorData.map { editorData =>
        Future.successful(Ok(views.html.editBehavior(editorData)))
      }.getOrElse {
        dataService.users.teamAccessFor(user, None).flatMap { teamAccess =>
          val response = NotFound(
            views.html.notFound(
              Some(teamAccess),
              Some("Behavior not found"),
              Some("The behavior you are trying to access could not be found."),
              Some(reAuthLinkFor(request, None))
            ))

          withAuthDiscarded(request, response)
        }
      }
    }
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
            val maybeDataTypeName = data.config.dataTypeName
            for {
              teamAccess <- dataService.users.teamAccessFor(user, Some(data.teamId))
              maybeBehavior <- data.behaviorId.map { behaviorId =>
                dataService.behaviors.find(behaviorId, user)
              }.getOrElse {
                teamAccess.maybeTargetTeam.map { team =>
                  dataService.behaviors.createFor(team, None, maybeDataTypeName).map(Some(_))
                }.getOrElse(Future.successful(None))
              }
              maybeBehaviorVersion <- maybeBehavior.map { behavior =>
                dataService.behaviorVersions.createFor(behavior, Some(user), data).map(Some(_))
              }.getOrElse(Future.successful(None))
              _ <- maybeBehavior.map { behavior =>
                if (behavior.maybeDataTypeName != maybeDataTypeName) {
                  dataService.behaviors.updateDataTypeNameFor(behavior, maybeDataTypeName)
                } else {
                  Future.successful({})
                }
              }.getOrElse(Future.successful({}))
              maybePreviousRequiredOAuth2ApiConfig <- info.maybeRequiredOAuth2ApiConfigId.map { id =>
                dataService.requiredOAuth2ApiConfigs.find(id)
              }.getOrElse(Future.successful(None))
              maybeRequiredOAuth2ApiConfig <- maybePreviousRequiredOAuth2ApiConfig.flatMap { config =>
                maybeBehaviorVersion.map { version =>
                  dataService.requiredOAuth2ApiConfigs.allFor(config.api, version).map(_.headOption)
                }
              }.getOrElse(Future.successful(None))
              maybeBehaviorVersionData <- maybeBehavior.map { behavior =>
                BehaviorVersionData.maybeFor(behavior.id, user, dataService)
              }.getOrElse(Future.successful(None))
            } yield {
              (for {
                behavior <- maybeBehavior
                behaviorVersionData <- maybeBehaviorVersionData
              } yield {
                if (info.maybeRedirect.contains("newOAuth2Application")) {
                  Redirect(routes.OAuth2ApplicationController.newApp(maybeRequiredOAuth2ApiConfig.map(_.id), Some(data.teamId), Some(behavior.id)))
                } else {
                  render {
                    case Accepts.Html() => Redirect(routes.BehaviorEditorController.edit(behavior.id, justSaved = Some(true)))
                    case Accepts.Json() => Ok(Json.toJson(behaviorVersionData))
                  }
                }
              }).getOrElse {
                NotFound(
                  views.html.notFound(
                    Some(teamAccess),
                    Some("Behavior not found"),
                    Some("The behavior you were trying to save could not be found."
                    )
                  )
                )
              }
            }
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
        for {
          maybeBehavior <- dataService.behaviors.find(behaviorId, request.identity)
          _ <- maybeBehavior.map { behavior =>
            dataService.behaviors.unlearn(behavior)
          }.getOrElse(Future.successful(Unit))
        } yield Redirect(routes.ApplicationController.index())
      }
    )
  }

  def versionInfoFor(behaviorId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      maybeBehavior <- dataService.behaviors.find(behaviorId, user)
      versions <- maybeBehavior.map { behavior =>
       dataService.behaviorVersions.allFor(behavior)
      }.getOrElse(Future.successful(Seq()))
      parametersByVersion <- Future.sequence(versions.map { version =>
        dataService.behaviorParameters.allFor(version).map { params =>
          (version, params)
        }
      }).map(_.toMap)
      triggersByVersion <- Future.sequence(versions.map { version =>
        dataService.messageTriggers.allFor(version).map { triggers =>
          (version, triggers)
        }
      }).map(_.toMap)
      awsConfigByVersion <- Future.sequence(versions.map { version =>
        dataService.awsConfigs.maybeFor(version).map { config =>
          (version, config)
        }
      }).map(_.toMap)
      requiredOAuth2ApiConfigsByVersion <- Future.sequence(versions.map { version =>
        dataService.requiredOAuth2ApiConfigs.allFor(version).map { apps =>
          (version, apps)
        }
      }).map(_.toMap)
      paramTypes <- Future.successful(parametersByVersion.flatMap { case(_, params) =>
        params.map(_.paramType)
      }.toSeq.distinct)
      paramTypeDataByParamTypes <- Future.sequence(paramTypes.map { paramType =>
        BehaviorParameterTypeData.from(paramType, dataService).map { data =>
          (paramType, data)
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
                BehaviorParameterData(ea.name, paramTypeDataByParamTypes.get(ea.paramType), ea.question)
              }
            }.getOrElse(Seq()),
            triggersByVersion.get(version).map { triggers =>
              triggers.map { ea =>
                BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
              }
            }.getOrElse(Seq()),
            BehaviorConfig(None, maybeAwsConfigData, maybeRequiredOAuth2ApiConfigsData, Some(version.forcePrivateResponse), behavior.maybeDataTypeName),
            behavior.maybeImportedId,
            None,
            Some(version.createdAt),
            dataService
          )
        }
        Ok(Json.toJson(versionsData))
      }.getOrElse {
        NotFound(Json.toJson("Error: behavior not found"))
      }
    }
  }

  case class TestTriggersInfo(behaviorId: String, message: String)

  private val testTriggersForm = Form(
    mapping(
      "behaviorId" -> nonEmptyText,
      "message" -> nonEmptyText
    )(TestTriggersInfo.apply)(TestTriggersInfo.unapply)
  )

  def testTriggers = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    testTriggersForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeBehavior <- dataService.behaviors.find(info.behaviorId, user)
          maybeBehaviorVersion <- maybeBehavior.map { behavior =>
            dataService.behaviors.maybeCurrentVersionFor(behavior)
          }.getOrElse(Future.successful(None))
          maybeReport <- maybeBehaviorVersion.map { behaviorVersion =>
            val context = TestMessageContext(user, behaviorVersion.team, info.message, includesBotMention = true)
            TriggerTester(lambdaService, dataService, cache).test(TestEvent(context), behaviorVersion).map(Some(_))
          }.getOrElse(Future.successful(None))

        } yield {
          maybeReport.map { report =>
            Ok(report.json)
          }.getOrElse {
            NotFound(s"Behavior not found: ${info.behaviorId}")
          }
        }
      }
    )
  }

  case class TestInvocationInfo(behaviorId: String, paramValuesJson: String)

  private val testInvocationForm = Form(
    mapping(
      "behaviorId" -> nonEmptyText,
      "paramValuesJson" -> nonEmptyText
    )(TestInvocationInfo.apply)(TestInvocationInfo.unapply)
  )

  def testInvocation = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    testInvocationForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.paramValuesJson)
        json.validate[Map[String, String]] match {
          case JsSuccess(paramValues, jsPath) => {
            for {
              maybeBehavior <- dataService.behaviors.find(info.behaviorId, user)
              maybeBehaviorVersion <- maybeBehavior.map { behavior =>
                dataService.behaviors.maybeCurrentVersionFor(behavior)
              }.getOrElse(Future.successful(None))
              maybeReport <- maybeBehaviorVersion.map { behaviorVersion =>
                InvocationTester(user, behaviorVersion, paramValues, lambdaService, dataService, cache).run.map(Some(_))
              }.getOrElse(Future.successful(None))
            } yield {
              maybeReport.map { report =>
                Ok(report.json)
              }.getOrElse {
                NotFound(s"Behavior not found: ${info.behaviorId}")
              }
            }
          }
          case JsError(err) => Future.successful(BadRequest(""))
        }
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
        for {
          maybeVersionData <- BehaviorVersionData.maybeFor(behaviorId, user, dataService)
          maybeTeam <- maybeVersionData.map { data =>
            dataService.teams.find(data.teamId, user)
          }.getOrElse(Future.successful(None))
          maybeImporter <- Future.successful(for {
            team <- maybeTeam
            data <- maybeVersionData
          } yield BehaviorVersionImporter(team, user, data, dataService))
          maybeCloned <- maybeImporter.map { importer =>
            importer.run.map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield maybeCloned.map { cloned =>
          Redirect(routes.BehaviorEditorController.edit(cloned.behavior.id))
        }.getOrElse {
          NotFound("")
        }
      }
    )
  }

  def regexValidationErrorsFor(pattern: String) = silhouette.SecuredAction { implicit request =>
    val content = MessageTrigger.maybeRegexValidationErrorFor(pattern).map { errMessage =>
      Array(errMessage)
    }.getOrElse {
      Array()
    }
    Ok(Json.toJson(Array(content)))
  }

}
