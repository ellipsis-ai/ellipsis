package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import json.Formatting._
import json._
import models.accounts.user.User
import models.behaviors.behaviorparameter.TextType
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
import play.api.mvc.{AnyContent, Result}
import play.filters.csrf.CSRF
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
                                           val ws: WSClient,
                                           val actorSystem: ActorSystem
                                         ) extends ReAuthable {

  def newGroup(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        editorDataResult(BehaviorEditorData.buildForNew(user, maybeTeamId, dataService, ws))
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
          result <- teamAccess.maybeTargetTeam.map { team =>
            val dataRoute = routes.BehaviorEditorController.newGroup(maybeTeamId)
            Future.successful(Ok(views.html.behavioreditor.edit(viewConfig(Some(teamAccess)), dataRoute)))
          }.getOrElse {
            reAuthFor(request, maybeTeamId)
          }
        } yield result
      }
    }
  }

  def edit(groupId: String, maybeBehaviorId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        editorDataResult(BehaviorEditorData.buildForEdit(user, groupId, maybeBehaviorId, dataService, ws))
      }
      case Accepts.Html() => {
        for {
          maybeGroupData <- BehaviorGroupData.maybeFor(groupId, user, maybeGithubUrl = None, dataService)
          maybeTeam <- maybeGroupData.map { data =>
            dataService.teams.find(data.teamId, user)
          }.getOrElse(Future.successful(None))
          result <- maybeTeam.map { team =>
            dataService.users.teamAccessFor(user, Some(team.id)).map { teamAccess =>
              val dataRoute = routes.BehaviorEditorController.edit(groupId, maybeBehaviorId)
              Ok(views.html.behavioreditor.edit(viewConfig(Some(teamAccess)), dataRoute))
            }
          }.getOrElse { skillNotFound }
        } yield result
      }
    }
  }

  private def editorDataResult(eventualMaybeEditorData: Future[Option[BehaviorEditorData]])(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    eventualMaybeEditorData.flatMap { maybeEditorData =>
      maybeEditorData.map { editorData =>
        val config = BehaviorEditorEditConfig.fromEditorData(
          containerId = "editorContainer",
          csrfToken = CSRF.getToken(request).map(_.value),
          data = editorData
        )
        Future.successful(Ok(views.js.shared.pageConfig(viewConfig(Some(editorData.teamAccess)), "config/behavioreditor/edit", Json.toJson(config))))
      }.getOrElse {
        Future.successful(NotFound("Skill not found"))
      }
    }
  }

  private def skillNotFound()(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    dataService.users.teamAccessFor(request.identity, None).flatMap { teamAccess =>
      val response = NotFound(
        views.html.error.notFound(
          viewConfig(Some(teamAccess)),
          Some("Skill not found"),
          Some("The skill you are trying to access could not be found."),
          Some(reAuthLinkFor(request, None))
        )
      )

      withAuthDiscarded(request, response)
    }
  }

  case class SaveBehaviorInfo(
                               dataJson: String,
                               isReinstall: Option[Boolean]
                             )

  private val saveForm = Form(
    mapping(
      "dataJson" -> nonEmptyText,
      "isReinstall" -> optional(boolean)
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
              oauth2Appications <- teamAccess.maybeTargetTeam.map { team =>
                dataService.oauth2Applications.allUsableFor(team)
              }.getOrElse(Future.successful(Seq()))
              _ <- maybeGroup.map { group =>
                val dataForNewVersion = data.copyForNewVersionOf(group)
                val dataToUse = if (info.isReinstall.exists(identity)) {
                  dataForNewVersion.copyWithApiApplicationsIfAvailable(oauth2Appications)
                } else {
                  dataForNewVersion
                }
                dataService.behaviorGroupVersions.createFor(group, user, dataToUse).map(Some(_))
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
                          maybeBehaviorIdToClone: Option[String],
                          maybeName: Option[String]
                        ) = silhouette.SecuredAction.async { implicit request =>
    maybeBehaviorIdToClone.map { behaviorIdToClone =>
      BehaviorVersionData.maybeFor(behaviorIdToClone, request.identity, dataService, None, None).map { maybeBehaviorVersionData =>
        maybeBehaviorVersionData.map(_.copyForClone)
      }
    }.getOrElse {
      Future.successful(Some(BehaviorVersionData.newUnsavedFor(teamId, isDataType, maybeName, dataService)))
    }.map { maybeVersionData =>
      maybeVersionData.map { data =>
        Ok(Json.toJson(data))
      }.getOrElse {
        NotFound(s"""Action not found: ${maybeBehaviorIdToClone.getOrElse("")}""")
      }
    }
  }

  def newUnsavedLibrary(
                          teamId: String,
                          maybeLibraryIdToClone: Option[String]
                        ) = silhouette.SecuredAction.async { implicit request =>
    maybeLibraryIdToClone.map { libraryIdToClone =>
      LibraryVersionData.maybeClonedFor(libraryIdToClone, dataService)
    }.getOrElse {
      Future.successful(Some(LibraryVersionData.newUnsaved))
    }.map { maybeVersionData =>
      maybeVersionData.map { data =>
        Ok(Json.toJson(data))
      }.getOrElse {
        NotFound(s"""Library not found: ${maybeLibraryIdToClone.getOrElse("")}""")
      }
    }
  }

  def versionInfoFor(behaviorGroupId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      maybeBehaviorGroup <- dataService.behaviorGroups.find(behaviorGroupId)
      versions <- maybeBehaviorGroup.map { group =>
       dataService.behaviorGroupVersions.allFor(group).map(_.sortBy(_.createdAt).reverse.take(20))
      }.getOrElse(Future.successful(Seq()))
      versionsData <- Future.sequence(versions.map { ea =>
       BehaviorGroupData.buildFor(ea, user, dataService)
      })
    } yield {
      Ok(Json.toJson(versionsData))
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
            TriggerTester(lambdaService, dataService, cache, ws, configuration, actorSystem).test(event, behaviorVersion).map(Some(_))
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
                InvocationTester(user, behaviorVersion, paramValues, lambdaService, dataService, cache, configuration, actorSystem).run.map(Some(_))
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
