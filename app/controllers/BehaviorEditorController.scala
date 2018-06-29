package controllers

import com.amazonaws.services.lambda.model.ResourceNotFoundException
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import javax.inject.Inject
import json.Formatting._
import json._
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.testing.{InvocationTester, TestEvent, TriggerTester}
import models.behaviors.triggers.messagetrigger.MessageTrigger
import models.silhouette.EllipsisEnv
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc.{AnyContent, Result}
import play.filters.csrf.CSRF
import services.{DefaultServices, GithubService}
import utils.FutureSequencer
import utils.github._

import scala.concurrent.{ExecutionContext, ExecutionException, Future}

class BehaviorEditorController @Inject() (
                                           val silhouette: Silhouette[EllipsisEnv],
                                           val githubService: GithubService,
                                           val services: DefaultServices,
                                           val assetsProvider: Provider[RemoteAssets],
                                           implicit val ec: ExecutionContext
                                         ) extends ReAuthable {

  val dataService = services.dataService
  val cacheService = services.cacheService
  val configuration = services.configuration
  val ws = services.ws

  def newGroup(maybeTeamId: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        editorDataResult(BehaviorEditorData.buildForNew(user, maybeTeamId, dataService, ws, assets), None)
      }
      case Accepts.Html() => {
        for {
          teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
        } yield teamAccess.maybeTargetTeam.map { team =>
          val dataRoute = routes.BehaviorEditorController.newGroup(maybeTeamId)
          Ok(views.html.behavioreditor.edit(viewConfig(Some(teamAccess)), dataRoute, "New skill"))
        }.getOrElse {
          notFoundWithLoginFor(request, Some(teamAccess))
        }
      }
    }
  }

  def edit(groupId: String, maybeBehaviorId: Option[String], maybeShowVersions: Option[Boolean]) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        editorDataResult(BehaviorEditorData.buildForEdit(user, groupId, maybeBehaviorId, dataService, cacheService, ws, assets), maybeShowVersions)
      }
      case Accepts.Html() => {
        for {
          maybeGroupData <- BehaviorGroupData.maybeFor(groupId, user, maybeGithubUrl = None, dataService, cacheService)
          maybeTeam <- maybeGroupData.map { data =>
            dataService.teams.find(data.teamId, user)
          }.getOrElse(Future.successful(None))
          result <- maybeTeam.map { team =>
            dataService.users.teamAccessFor(user, Some(team.id)).map { teamAccess =>
              val skillTitle = maybeGroupData.flatMap(_.name).getOrElse("Untitled skill")
              val dataRoute = routes.BehaviorEditorController.edit(groupId, maybeBehaviorId, maybeShowVersions)
              Ok(views.html.behavioreditor.edit(viewConfig(Some(teamAccess)), dataRoute, skillTitle))
            }
          }.getOrElse { skillNotFound }
        } yield result
      }
    }
  }

  private def editorDataResult(eventualMaybeEditorData: Future[Option[BehaviorEditorData]], maybeShowVersions: Option[Boolean])(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    eventualMaybeEditorData.flatMap { maybeEditorData =>
      maybeEditorData.map { editorData =>
        val config = BehaviorEditorEditConfig.fromEditorData(
          containerId = "editorContainer",
          csrfToken = CSRF.getToken(request).map(_.value),
          data = editorData,
          maybeShowVersions
        )
        Future.successful(Ok(views.js.shared.webpackLoader(
          viewConfig(Some(editorData.teamAccess)),
          "BehaviorEditorConfiguration",
          "behaviorEditor",
          Json.toJson(config)
        )))
      }.getOrElse {
        Future.successful(NotFound("Skill not found"))
      }
    }
  }

  def metaData(behaviorGroupId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      maybeBehaviorGroup <- dataService.behaviorGroups.find(behaviorGroupId, user)
      maybeLastVersion <- maybeBehaviorGroup.map { group =>
        dataService.behaviorGroupVersions.maybeCurrentFor(group)
      }.getOrElse(Future.successful(None))
      maybeUserData <- maybeLastVersion.flatMap { version =>
        version.maybeAuthor.map { author =>
          dataService.users.userDataFor(author, version.team).map(Some(_))
        }
      }.getOrElse(Future.successful(None))
    } yield {
      maybeLastVersion.map { groupVersion =>
        Ok(Json.toJson(BehaviorGroupVersionMetaData(behaviorGroupId, groupVersion.createdAt, maybeUserData)))
      }.getOrElse {
        NotFound("Skill not found")
      }
    }
  }

  private def skillNotFound()(implicit request: SecuredRequest[EllipsisEnv, AnyContent]): Future[Result] = {
    dataService.users.teamAccessFor(request.identity, None).map { teamAccess =>
      notFoundWithLoginFor(
        request,
        Some(teamAccess),
        Some("Skill not found"),
        Some("The skill you are trying to access could not be found.")
      )
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
                dataService.behaviorGroups.findWithoutAccessCheck(groupId)
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
                BehaviorGroupData.maybeFor(group.id, user, maybeGithubUrl = None, dataService, cacheService)
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

  case class UpdateNodeModulesInfo(behaviorGroupId: String)

  private val updateNodeModulesForm = Form(
    mapping(
      "behaviorGroupId" -> nonEmptyText
    )(UpdateNodeModulesInfo.apply)(UpdateNodeModulesInfo.unapply)
  )

  def updateNodeModules = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    updateNodeModulesForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeGroup <- dataService.behaviorGroups.find(info.behaviorGroupId, user)
          maybeGroupData <- BehaviorGroupData.maybeFor(info.behaviorGroupId, user, maybeGithubUrl = None, dataService, cacheService)
          maybeSavedGroupVersion <- (for {
            group <- maybeGroup
            groupData <- maybeGroupData
          } yield {
            dataService.behaviorGroupVersions.createFor(group, user, groupData.copyForNewVersionOf(group)).map(Some(_))
          }).getOrElse(Future.successful(None))
          maybeUpdatedGroupData <- maybeSavedGroupVersion.map { groupVersion =>
            BehaviorGroupData.buildFor(groupVersion, user, None, dataService, cacheService).map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield {
          maybeUpdatedGroupData.map { groupData =>
            Ok(Json.toJson(groupData))
          }.getOrElse {
            NotFound("")
          }
        }
      }
    )
  }

  def newUnsavedBehavior(
                          isDataType: Boolean,
                          isTest: Boolean,
                          teamId: String,
                          maybeBehaviorIdToClone: Option[String],
                          maybeName: Option[String]
                        ) = silhouette.SecuredAction.async { implicit request =>
    maybeBehaviorIdToClone.map { behaviorIdToClone =>
      BehaviorVersionData.maybeFor(behaviorIdToClone, request.identity, dataService, None, None).map { maybeBehaviorVersionData =>
        maybeBehaviorVersionData.map(_.copyForClone)
      }
    }.getOrElse {
      Future.successful(Some(BehaviorVersionData.newUnsavedFor(teamId, isDataType, isTest, maybeName, dataService)))
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
      maybeBehaviorGroup <- dataService.behaviorGroups.findWithoutAccessCheck(behaviorGroupId)
      versions <- maybeBehaviorGroup.map { group =>
       dataService.behaviorGroupVersions.batchFor(group)
      }.getOrElse(Future.successful(Seq()))
      // Todo: this can go back to being a regular Future.sequence (in parallel) if we
      versionsData <- FutureSequencer.sequence(versions, (ea: BehaviorGroupVersion) => BehaviorGroupData.buildFor(ea, user, None, dataService, cacheService))
    } yield {
      Ok(Json.toJson(versionsData))
    }
  }

  def nodeModuleVersionsFor(behaviorGroupId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    for {
      maybeBehaviorGroup <- dataService.behaviorGroups.find(behaviorGroupId, user)
      maybeCurrentGroupVersion <- maybeBehaviorGroup.map { group =>
        dataService.behaviorGroups.maybeCurrentVersionFor(group)
      }.getOrElse(Future.successful(None))
      nodeModuleVersions <- maybeCurrentGroupVersion.map { groupVersion =>
        dataService.run(services.lambdaService.ensureNodeModuleVersionsFor(groupVersion))
      }.getOrElse(Future.successful(Seq()))
    } yield {
      Ok(Json.toJson(nodeModuleVersions.map(NodeModuleVersionData.from)))
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
            TriggerTester(services).test(event, behaviorVersion).map(Some(_))
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
                InvocationTester(user, behaviorVersion, paramValues, services).run.map(Some(_))
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

  case class SaveDefaultStorageItemInfo(itemJson: String)

  private val saveDefaultStorageItemForm = Form(
    mapping(
      "itemJson" -> nonEmptyText
    )(SaveDefaultStorageItemInfo.apply)(SaveDefaultStorageItemInfo.unapply)
  )

  def saveDefaultStorageItem = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    saveDefaultStorageItemForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.itemJson)
        json.validate[DefaultStorageItemData] match {
          case JsSuccess(item, _) => {
            for {
              maybeBehavior <- dataService.behaviors.find(item.behaviorId, user)
              result <- maybeBehavior.map { behavior =>
                dataService.defaultStorageItems.createItemForBehavior(behavior, user, item.data).map { newItem =>
                  Ok(Json.toJson(DefaultStorageItemData.fromItem(newItem)))
                }
              }.getOrElse(Future.successful(NotFound(s"Couldn't find data type for ID: ${item.behaviorId}")))
            } yield result
          }
          case JsError(errs) => Future.successful(BadRequest("Couldn't build a storage item from this data"))
        }
      }
    )
  }

  case class DeleteDefaultStorageItemsInfo(behaviorId: String, itemIds: Seq[String])

  private val deleteDefaultStorageItemsForm = Form(
    mapping(
      "behaviorId" -> nonEmptyText,
      "itemIds" -> seq(nonEmptyText)
    )(DeleteDefaultStorageItemsInfo.apply)(DeleteDefaultStorageItemsInfo.unapply)
  )

  def deleteDefaultStorageItems = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    deleteDefaultStorageItemsForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeBehavior <- dataService.behaviors.find(info.behaviorId, user)
          result <- maybeBehavior.map { behavior =>
            dataService.defaultStorageItems.deleteItems(info.itemIds, behavior.group).map { count =>
              Ok(Json.toJson(Map("deletedCount" -> count)))
            }
          }.getOrElse(Future.successful(NotFound(s"Couldn't find data type for ID: ${info.behaviorId}")))
        } yield result
      }
    )
  }

  case class QueryDefaultStorageInfo(behaviorGroupId: String, query: String, maybeOperationName: Option[String], maybeVariables: Option[String])

  private val queryDefaultStorageForm = Form(
    mapping(
      "behaviorGroupId" -> nonEmptyText,
      "query" -> nonEmptyText,
      "operationName" -> optional(nonEmptyText),
      "variables" -> optional(nonEmptyText)
    )(QueryDefaultStorageInfo.apply)(QueryDefaultStorageInfo.unapply)
  )

  def queryDefaultStorage = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    queryDefaultStorageForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeBehaviorGroup <- dataService.behaviorGroups.findWithoutAccessCheck(info.behaviorGroupId)
          maybeResult <- maybeBehaviorGroup.map { group =>
            services.graphQLService.runQuery(group, user, info.query, info.maybeOperationName, info.maybeVariables).map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield {
          maybeResult.map { result =>
            Ok(result.toString)
          }.getOrElse(NotFound("Skill not found"))
        }
      }
    )
  }


  case class GithubActionErrorData(message: String, `type`: Option[String], details: Option[JsObject])

  case class GithubActionErrorResponse(errors: GithubActionErrorData)
  object GithubActionErrorResponse {
    def jsonFrom(message: String, `type`: Option[String], details: Option[JsObject]): JsValue = {
      Json.toJson(GithubActionErrorResponse(GithubActionErrorData(message, `type`, details)))
    }
  }
  implicit val githubActionErrorDataWrites = Json.writes[GithubActionErrorData]
  implicit val githubActionErrorResponseWrites = Json.writes[GithubActionErrorResponse]

  case class UpdateFromGithubInfo(
                                   behaviorGroupId: String,
                                   owner: String,
                                   repo: String,
                                   branch: Option[String]
                                 )

  private val updateFromGithubForm = Form(
    mapping(
      "behaviorGroupId" -> nonEmptyText,
      "owner" -> nonEmptyText,
      "repo" -> nonEmptyText,
      "branch" -> optional(nonEmptyText)
    )(UpdateFromGithubInfo.apply)(UpdateFromGithubInfo.unapply)
  )

  case class UpdateFromGithubSuccessResponse(data: BehaviorGroupData)

  implicit val updateFromGithubSuccessResponseWrites = Json.writes[UpdateFromGithubSuccessResponse]

  def updateFromGithub = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    updateFromGithubForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeGithubLinkedAccount <- dataService.linkedAccounts.maybeForGithubFor(user)
          maybeGithubProfile <- maybeGithubLinkedAccount.map { linked =>
            dataService.githubProfiles.find(linked.loginInfo)
          }.getOrElse(Future.successful(None))
          maybeBehaviorGroup <- dataService.behaviorGroups.find(info.behaviorGroupId, user)
          _ <- dataService.linkedGithubRepos.maybeSetCurrentBranch(maybeBehaviorGroup, info.branch)
          maybeExistingGroupData <- maybeBehaviorGroup.map { group =>
            dataService.behaviorGroups.maybeCurrentVersionFor(group).flatMap { maybeCurrentVersion =>
              maybeCurrentVersion.map { currentVersion =>
                BehaviorGroupData.buildFor(currentVersion, user, None, dataService, cacheService).map(Some(_))
              }.getOrElse(Future.successful(None))
            }
          }.getOrElse(Future.successful(None))
          teamAccess <- dataService.users.teamAccessFor(user, maybeBehaviorGroup.map(_.team.id))
          oauth2Appications <- teamAccess.maybeTargetTeam.map { team =>
            dataService.oauth2Applications.allUsableFor(team)
          }.getOrElse(Future.successful(Seq()))
        } yield {
          maybeBehaviorGroup.map { group =>
            maybeGithubProfile.map { profile =>
              val fetcher = GithubSingleBehaviorGroupFetcher(group.team, info.owner, info.repo, profile.token, info.branch, maybeExistingGroupData, githubService, services, ec)
              try {
                val groupData = fetcher.result.copyWithApiApplicationsIfAvailable(oauth2Appications)
                Ok(Json.toJson(UpdateFromGithubSuccessResponse(groupData)))
              } catch {
                case e: GithubResultFromDataException => Ok(GithubActionErrorResponse.jsonFrom(e.getMessage, Some(e.exceptionType.toString), Some(e.details)))
                case e: GithubFetchDataException => Ok(GithubActionErrorResponse.jsonFrom(e.getMessage, None, None))
              }
            }.getOrElse(Unauthorized(s"User is not correctly authed with GitHub"))
          }.getOrElse(NotFound(s"Skill with ID ${info.behaviorGroupId} not found"))
        }
      }
    )
  }

  case class PushToGithubInfo(
                               behaviorGroupId: String,
                               owner: String,
                               repo: String,
                               branch: Option[String],
                               commitMessage: String
                             )

  private val pushToGithubForm = Form(
    mapping(
      "behaviorGroupId" -> nonEmptyText,
      "owner" -> nonEmptyText,
      "repo" -> nonEmptyText,
      "branch" -> optional(nonEmptyText),
      "commitMessage" -> nonEmptyText
    )(PushToGithubInfo.apply)(PushToGithubInfo.unapply)
  )

  case class PushToGithubSuccessData(branch: String)
  case class PushToGithubSuccessResponse(data: PushToGithubSuccessData)

  implicit val pushToGithubSuccessDataWrites = Json.writes[PushToGithubSuccessData]
  implicit val pushToGithubSuccessResponseWrites = Json.writes[PushToGithubSuccessResponse]

  def pushToGithub = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    pushToGithubForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeGithubLinkedAccount <- dataService.linkedAccounts.maybeForGithubFor(user)
          maybeGithubProfile <- maybeGithubLinkedAccount.map { linked =>
            dataService.githubProfiles.find(linked.loginInfo)
          }.getOrElse(Future.successful(None))
          maybeBehaviorGroup <- dataService.behaviorGroups.find(info.behaviorGroupId, user)
          _ <- dataService.linkedGithubRepos.maybeSetCurrentBranch(maybeBehaviorGroup, info.branch)
          result <- maybeBehaviorGroup.map { group =>
            maybeGithubProfile.map { profile =>
              val committerInfo = GithubCommitterInfoFetcher(user, profile.token, githubService, services, ec).result
              val branch = info.branch.getOrElse("master")
              val pusher =
                GithubPusher(
                  info.owner,
                  info.repo,
                  branch,
                  info.commitMessage,
                  profile.token,
                  committerInfo,
                  group,
                  user,
                  services,
                  ec
                )
              pusher.run.map { r =>
                Ok(Json.toJson(PushToGithubSuccessResponse(PushToGithubSuccessData(branch))))
              }.recover {
                case e: GitPushException => {
                  Ok(GithubActionErrorResponse.jsonFrom(e.getMessage, Some(e.exceptionType.toString), Some(e.details)))
                }
                case e: GitCommandException => {
                  BadRequest(e.getMessage)
                }
              }
            }.getOrElse(Future.successful(Unauthorized(s"User is not correctly authed with GitHub")))
          }.getOrElse(Future.successful(NotFound(s"Skill with ID ${info.behaviorGroupId} not found")))
        } yield result
      }
    )
  }

  case class LinkToGithubRepoInfo(
                               behaviorGroupId: String,
                               owner: String,
                               repo: String,
                               currentBranch: Option[String]
                             )

  private val linkToGithubRepoForm = Form(
    mapping(
      "behaviorGroupId" -> nonEmptyText,
      "owner" -> nonEmptyText,
      "repo" -> nonEmptyText,
      "currentBranch" -> optional(nonEmptyText)
    )(LinkToGithubRepoInfo.apply)(LinkToGithubRepoInfo.unapply)
  )

  def linkToGithubRepo = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    linkToGithubRepoForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeGithubLinkedAccount <- dataService.linkedAccounts.maybeForGithubFor(user)
          maybeGithubProfile <- maybeGithubLinkedAccount.map { linked =>
            dataService.githubProfiles.find(linked.loginInfo)
          }.getOrElse(Future.successful(None))
          maybeBehaviorGroup <- dataService.behaviorGroups.find(info.behaviorGroupId, user)
          result <- maybeBehaviorGroup.map { group =>
            maybeGithubProfile.map { profile =>
              dataService.linkedGithubRepos.ensureLink(group, info.owner, info.repo, info.currentBranch).map { linked =>
                Ok(Json.toJson(LinkedGithubRepoData(linked.owner, linked.repo, linked.maybeCurrentBranch)))
              }
            }.getOrElse(Future.successful(Unauthorized(s"User is not correctly authed with GitHub")))
          }.getOrElse(Future.successful(NotFound(s"Skill with ID ${info.behaviorGroupId} not found")))
        } yield result
      }
    )
  }

  case class DeployInfo(
                         behaviorGroupId: String,
                         comment: Option[String]
                       )

  private val deployForm = Form(
    mapping(
      "behaviorGroupId" -> nonEmptyText,
      "comment" -> optional(nonEmptyText)
    )(DeployInfo.apply)(DeployInfo.unapply)
  )

  def deploy = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    deployForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        for {
          maybeGroup <- dataService.behaviorGroups.find(info.behaviorGroupId, user)
          maybeCurrentGroupVersion <- maybeGroup.map { group =>
            dataService.behaviorGroups.maybeCurrentVersionFor(group)
          }.getOrElse(Future.successful(None))
          maybeDeployment <- maybeCurrentGroupVersion.map { groupVersion =>
            dataService.behaviorGroupDeployments.deploy(groupVersion, user.id, None).map(Some(_))
          }.getOrElse(Future.successful(None))
          maybeDeploymentData <- maybeDeployment.map { deployment =>
            BehaviorGroupDeploymentData.fromDeployment(deployment, dataService).map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield maybeDeploymentData.map { deployment =>
          Ok(Json.toJson(deployment))
        }.getOrElse {
          NotFound(s"Couldn't find skill with ID: ${info.behaviorGroupId}")
        }
      }
    )
  }

  def testResults(groupId: String) = silhouette.SecuredAction.async { implicit request =>
    (for {
      maybeGroup <- dataService.behaviorGroups.find(groupId, request.identity)
      maybeGroupVersion <- maybeGroup.map { group =>
        dataService.behaviorGroupVersions.maybeCurrentFor(group)
      }.getOrElse(Future.successful(None))
      tests <- maybeGroupVersion.map { groupVersion =>
        dataService.behaviorVersions.allForGroupVersion(groupVersion).map(_.filter(_.isTest))
      }.getOrElse(Future.successful(Seq()))
      results <- Future.sequence(tests.map { ea =>
        dataService.behaviorTestResults.ensureFor(ea).map(Some(_)).recover {
          case e: ExecutionException => {
            e.getCause match {
              case r: ResourceNotFoundException => None
              case _ => throw e
            }
          }
          case o => throw o
        }
      })
    } yield {
      Ok(Json.toJson(BehaviorTestResultsData(
        results.flatten,
        results.exists(_.isEmpty)
      )))
    }).recover {
      case e: Exception => InternalServerError(e.getMessage)
    }
  }
}
