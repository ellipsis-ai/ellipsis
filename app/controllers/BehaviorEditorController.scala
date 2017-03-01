package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import export.BehaviorVersionImporter
import json._
import json.Formatting._
import models.accounts.user.User
import models.behaviors.testing.{InvocationTester, TestEvent, TriggerTester}
import models.behaviors.triggers.messagetrigger.MessageTrigger
import models.silhouette.EllipsisEnv
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BehaviorEditorController @Inject() (
                                           val messagesApi: MessagesApi,
                                           val silhouette: Silhouette[EllipsisEnv],
                                           val configuration: Configuration,
                                           val dataService: DataService,
                                           val lambdaService: AWSLambdaService,
                                           val cache: CacheApi,
                                           val ws: WSClient
                                         ) extends ReAuthable {

  def newGroup(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    BehaviorEditorData.buildForNew(user, maybeTeamId, dataService, ws).flatMap { maybeEditorData =>
      maybeEditorData.map { editorData =>
        Future.successful(Ok(views.html.editBehavior(viewConfig(Some(editorData.teamAccess)), editorData)))
      }.getOrElse {
        dataService.users.teamAccessFor(user, None).flatMap { teamAccess =>
          val response = NotFound(
            views.html.notFound(
              viewConfig(Some(teamAccess)),
              Some(""),
              Some("The skill you are trying to access could not be found."),
              Some(reAuthLinkFor(request, None))
            ))

          withAuthDiscarded(request, response)
        }
      }
    }
  }

  def edit(groupId: String, maybeBehaviorId: Option[String], maybeJustSaved: Option[Boolean]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    BehaviorEditorData.buildForEdit(user, groupId, maybeBehaviorId, maybeJustSaved, dataService, ws).flatMap { maybeEditorData =>
      maybeEditorData.map { editorData =>
        Future.successful(Ok(views.html.editBehavior(viewConfig(Some(editorData.teamAccess)), editorData)))
      }.getOrElse {
        dataService.users.teamAccessFor(user, None).flatMap { teamAccess =>
          val response = NotFound(
            views.html.notFound(
              viewConfig(Some(teamAccess)),
              Some("Skill not found"),
              Some("The skill you are trying to access could not be found."),
              Some(reAuthLinkFor(request, None))
            ))

          withAuthDiscarded(request, response)
        }
      }
    }
  }

  case class SaveBehaviorInfo(
                               dataJson: String
                             )

  private val saveForm = Form(
    mapping(
      "dataJson" -> nonEmptyText
    )(SaveBehaviorInfo.apply)(SaveBehaviorInfo.unapply)
  )

  private def saveBehavior(data: BehaviorVersionData, user: User): Future[Option[BehaviorVersionData]] = {
    val maybeDataTypeName = data.config.dataTypeName
    for {
      teamAccess <- dataService.users.teamAccessFor(user, Some(data.teamId))
      maybeBehaviorGroup <- data.groupId.map { groupId =>
        dataService.behaviorGroups.find(groupId)
      }.getOrElse(Future.successful(None))
      maybeBehavior <- data.behaviorId.map { behaviorId =>
        dataService.behaviors.find(behaviorId, user)
      }.getOrElse {
        teamAccess.maybeTargetTeam.map { team =>
          maybeBehaviorGroup.map { behaviorGroup =>
            dataService.behaviors.createFor(behaviorGroup, None, maybeDataTypeName).map(Some(_))
          }.getOrElse {
            dataService.behaviors.createFor(team, None, maybeDataTypeName).map(Some(_))
          }
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
      maybeBehaviorVersionData <- maybeBehavior.map { behavior =>
        BehaviorVersionData.maybeFor(behavior.id, user, dataService)
      }.getOrElse(Future.successful(None))
    } yield maybeBehaviorVersionData
  }

  def save = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    saveForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[BehaviorGroupData] match {
          case JsSuccess(data, jsPath) => {
            for {
              behaviorVersions <- Future.sequence(data.behaviorVersions.map { ea =>
                saveBehavior(ea, user)
              }).map(_.flatten)
              maybeGroupData <- behaviorVersions.headOption.flatMap { version =>
                version.groupId.map { groupId =>
                  BehaviorGroupData.maybeFor(groupId, user, maybeGithubUrl = None, dataService)
                }
              }.getOrElse(Future.successful(None))
            } yield {
              maybeGroupData.map { groupData =>
                Ok(Json.toJson(groupData))
              }.getOrElse {
                NotFound("")
              }
            }
          }
          case e: JsError => {
            Future.successful(BadRequest(s"Malformatted data: ${e.errors.mkString("\n")}"))
          }
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
          otherBehaviorsInGroup <- maybeBehavior.map { behavior =>
            dataService.behaviors.allForGroup(behavior.group).map { all =>
              all.diff(Seq(behavior))
            }
          }.getOrElse(Future.successful(Seq()))
          _ <- maybeBehavior.map { behavior =>
            dataService.behaviors.unlearn(behavior)
          }.getOrElse(Future.successful(Unit))
        } yield {
          val redirect = otherBehaviorsInGroup.headOption.map { otherBehavior =>
            routes.BehaviorEditorController.edit(otherBehavior.group.id, Some(otherBehavior.id))
          }.getOrElse {
            routes.ApplicationController.index()
          }
          Redirect(redirect)
        }
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
      requiredSimpleTokenApisByVersion <- Future.sequence(versions.map { version =>
        dataService.requiredSimpleTokenApis.allFor(version).map { apis =>
          (version, apis)
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
          val maybeRequiredSimpleTokenApisData = requiredSimpleTokenApisByVersion.get(version).map { apis =>
            apis.map(ea => RequiredSimpleTokenApiData.from(ea))
          }
          BehaviorVersionData.buildFor(
            version.team.id,
            behavior.maybeGroup.map(_.id),
            Some(behavior.id),
            version.maybeDescription,
            version.functionBody,
            version.maybeResponseTemplate.getOrElse(""),
            parametersByVersion.get(version).map { params =>
              params.map { ea =>
                BehaviorParameterData(
                  ea.name,
                  paramTypeDataByParamTypes.get(ea.paramType),
                  ea.question,
                  Some(ea.input.isSavedForTeam),
                  Some(ea.input.isSavedForUser),
                  Some(ea.input.id),
                  ea.input.maybeExportId,
                  ea.input.maybeBehaviorGroup.map(_.id)
                )
              }
            }.getOrElse(Seq()),
            triggersByVersion.get(version).map { triggers =>
              triggers.map { ea =>
                BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
              }
            }.getOrElse(Seq()),
            BehaviorConfig(
              None,
              version.maybeName,
              maybeAwsConfigData,
              maybeRequiredOAuth2ApiConfigsData,
              maybeRequiredSimpleTokenApisData,
              Some(version.forcePrivateResponse),
              behavior.maybeDataTypeName
            ),
            behavior.maybeExportId,
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
            val event = TestEvent(user, behaviorVersion.team, info.message, includesBotMention = true)
            TriggerTester(lambdaService, dataService, cache, ws, configuration).test(event, behaviorVersion).map(Some(_))
          }.getOrElse(Future.successful(None))

        } yield {
          maybeReport.map { report =>
            Ok(report.json)
          }.getOrElse {
            NotFound(s"Skill not found: ${info.behaviorId}")
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
                InvocationTester(user, behaviorVersion, paramValues, lambdaService, dataService, cache, configuration).run.map(Some(_))
              }.getOrElse(Future.successful(None))
            } yield {
              maybeReport.map { report =>
                Ok(report.json)
              }.getOrElse {
                NotFound(s"Skill not found: ${info.behaviorId}")
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
            importer.run
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

  case class BehaviorGroupNameInfo(groupId: String, name: String)

  private val saveBehaviorGroupNameForm = Form(
    mapping(
      "groupId" -> nonEmptyText,
      "name" -> text
    )(BehaviorGroupNameInfo.apply)(BehaviorGroupNameInfo.unapply)
  )

  def saveBehaviorGroupName = silhouette.SecuredAction.async { implicit request =>
    saveBehaviorGroupNameForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeBehaviorGroup <- dataService.behaviorGroups.find(info.groupId)
          maybeSaved <- maybeBehaviorGroup.map { group =>
            dataService.behaviorGroups.save(group.copy(name = info.name)).map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield {
          maybeSaved.map { saved =>
            Ok("Success")
          }.getOrElse {
            NotFound(s"Skill not found: ${info.groupId}")
          }
        }
      }
    )
  }

  case class BehaviorGroupDescriptionInfo(groupId: String, description: String)

  private val saveBehaviorGroupDescriptionForm = Form(
    mapping(
      "groupId" -> nonEmptyText,
      "description" -> text
    )(BehaviorGroupDescriptionInfo.apply)(BehaviorGroupDescriptionInfo.unapply)
  )

  def saveBehaviorGroupDescription = silhouette.SecuredAction.async { implicit request =>
    saveBehaviorGroupDescriptionForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeBehaviorGroup <- dataService.behaviorGroups.find(info.groupId)
          maybeSaved <- maybeBehaviorGroup.map { group =>
            dataService.behaviorGroups.save(group.copy(maybeDescription = Some(info.description))).map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield {
          maybeSaved.map { saved =>
            Ok("Success")
          }.getOrElse {
            NotFound(s"Skill not found: ${info.groupId}")
          }
        }
      }
    )
  }

}
