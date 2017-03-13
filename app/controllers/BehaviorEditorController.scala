package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import json.Formatting._
import json._
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

  def edit(groupId: String, maybeBehaviorId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    BehaviorEditorData.buildForEdit(user, groupId, maybeBehaviorId, dataService, ws).flatMap { maybeEditorData =>
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
              teamAccess <- dataService.users.teamAccessFor(user, Some(data.teamId))
              maybeExistingGroup <- data.id.map { groupId =>
                dataService.behaviorGroups.find(groupId)
              }.getOrElse(Future.successful(None))
              maybeGroup <- maybeExistingGroup.map(g => Future.successful(Some(g))).getOrElse {
                teamAccess.maybeTargetTeam.map { team =>
                  dataService.behaviorGroups.createFor(data.exportId, team).map(Some(_))
                }.getOrElse(Future.successful(None))
              }
              _ <- maybeGroup.map { group =>
                dataService.behaviorGroupVersions.createFor(group, user, data.copyForNewVersionOf(group)).map(Some(_))
              }.getOrElse(Future.successful(None))
              maybeGroupData <- maybeGroup.map { group =>
                BehaviorGroupData.maybeFor(group.id, user, maybeGithubUrl = None, dataService)
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

  def newUnsavedBehavior(
                          isDataType: Boolean,
                          teamId: String,
                          maybeBehaviorIdToClone: Option[String]
                        ) = silhouette.SecuredAction.async { implicit request =>
    maybeBehaviorIdToClone.map { behaviorIdToClone =>
      BehaviorVersionData.maybeFor(behaviorIdToClone, request.identity, dataService, None).map { maybeBehaviorVersionData =>
        maybeBehaviorVersionData.map(_.copyForClone)
      }
    }.getOrElse {
      Future.successful(Some(BehaviorVersionData.newUnsavedFor(teamId, isDataType, dataService)))
    }.map { maybeVersionData =>
      maybeVersionData.map { data =>
        Ok(Json.toJson(data))
      }.getOrElse {
        NotFound(s"""Action not found: ${maybeBehaviorIdToClone.getOrElse("")}""")
      }
    }
  }

  def versionInfoFor(behaviorGroupId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      maybeBehaviorGroup <- dataService.behaviorGroups.find(behaviorGroupId)
      versions <- maybeBehaviorGroup.map { group =>
       dataService.behaviorGroupVersions.allFor(group)
      }.getOrElse(Future.successful(Seq()))
      versionsData <- Future.sequence(versions.map { ea =>
        BehaviorGroupData.buildFor(ea, user, dataService)
      })
    } yield {
      Ok(Json.toJson(versionsData.sortBy(_.createdAt).reverse))
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

  def regexValidationErrorsFor(pattern: String) = silhouette.SecuredAction { implicit request =>
    val content = MessageTrigger.maybeRegexValidationErrorFor(pattern).map { errMessage =>
      Array(errMessage)
    }.getOrElse {
      Array()
    }
    Ok(Json.toJson(Array(content)))
  }

}
