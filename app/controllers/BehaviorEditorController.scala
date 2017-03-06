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
                dataService.behaviorGroupVersions.createFor(group, user, data).map(Some(_))
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

  def newUnsavedBehavior(isDataType: Boolean, teamId: String, maybeGroupId: Option[String]) = silhouette.SecuredAction { implicit request =>
    val data = BehaviorVersionData.newUnsavedFor(teamId, maybeGroupId, isDataType, dataService)
    Ok(Json.toJson(data))
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
          maybeExistingBehavior <- dataService.behaviors.find(behaviorId, user)
          maybeExistingGroupData <- maybeExistingBehavior.map { behavior =>
            BehaviorGroupData.maybeFor(behavior.group.id, user, None, dataService)
          }.getOrElse(Future.successful(None))
          maybeVersionData <- BehaviorVersionData.maybeFor(behaviorId, user, dataService, None).map { maybeVersionData =>
            maybeVersionData.map(_.copyForClone)
          }
          maybeNewGroupData <- Future.successful(for {
            groupData <- maybeExistingGroupData
            versionData <- maybeVersionData
          } yield {
            groupData.copy(behaviorVersions = groupData.behaviorVersions ++ Seq(versionData))
          })
          maybeGroup <- (for {
            groupData <- maybeExistingGroupData
            groupId <- groupData.id
          } yield {
            dataService.behaviorGroups.find(groupId)
          }).getOrElse(Future.successful(None))
          maybeNewGroupVersion <- (for {
            newGroupData <- maybeNewGroupData
            group <- maybeGroup
          } yield {
            dataService.behaviorGroupVersions.createFor(group, user, newGroupData).map(Some(_))
          }).getOrElse(Future.successful(None))
        } yield {
          (for {
            newGroupVersion <- maybeNewGroupVersion
            newBehaviorId <- maybeVersionData.map(_.behaviorId)
          } yield {
            Redirect(routes.BehaviorEditorController.edit(newGroupVersion.group.id, newBehaviorId))
          }).getOrElse {
            NotFound("")
          }
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
