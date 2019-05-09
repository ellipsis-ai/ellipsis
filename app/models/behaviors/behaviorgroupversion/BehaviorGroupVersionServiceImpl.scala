package models.behaviors.behaviorgroupversion

import java.time.OffsetDateTime

import javax.inject.Inject
import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.{BehaviorGroupData, LinkedGithubRepoData}
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import play.api.Logger
import services.{AWSLambdaService, ApiConfigInfo, DataService, DefaultServices}
import slick.dbio.DBIO

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

case class RawBehaviorGroupVersion(
                                   id: String,
                                   groupId: String,
                                   name: String,
                                   maybeIcon: Option[String],
                                   maybeDescription: Option[String],
                                   maybeAuthorId: Option[String],
                                   createdAt: OffsetDateTime
                                 )

class BehaviorGroupVersionsTable(tag: Tag) extends Table[RawBehaviorGroupVersion](tag, "behavior_group_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def groupId = column[String]("group_id")
  def name = column[String]("name")
  def maybeIcon = column[Option[String]]("icon")
  def maybeDescription = column[Option[String]]("description")
  def maybeAuthorId = column[Option[String]]("author_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, groupId, name, maybeIcon, maybeDescription, maybeAuthorId, createdAt) <>
      ((RawBehaviorGroupVersion.apply _).tupled, RawBehaviorGroupVersion.unapply _)
}

class BehaviorGroupVersionServiceImpl @Inject() (
                                                  defaultServicesProvider: Provider[DefaultServices],
                                                  lambdaServiceProvider: Provider[AWSLambdaService],
                                                  implicit val ec: ExecutionContext
                                                ) extends BehaviorGroupVersionService {

  def defaultServices: DefaultServices = defaultServicesProvider.get
  def dataService = defaultServices.dataService
  def lambdaService = defaultServices.lambdaService
  def cacheService = defaultServices.cacheService

  import BehaviorGroupVersionQueries._

  def findWithoutAccessCheckAction(id: String): DBIO[Option[BehaviorGroupVersion]] = {
    findQuery(id).result.map(_.headOption.map(tuple2BehaviorGroupVersion))
  }

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorGroupVersion]] = {
    dataService.run(findWithoutAccessCheckAction(id))
  }

  def batchFor(group: BehaviorGroup, batchSize: Int = 20, offset: Int = 0): Future[Seq[BehaviorGroupVersion]] = {
    val action = batchForQuery(group.id, batchSize, offset).result.map { r =>
      r.map(tuple2BehaviorGroupVersion)
    }
    dataService.run(action)
  }

  def maybeCurrentForAction(group: BehaviorGroup): DBIO[Option[BehaviorGroupVersion]] = {
    currentIdForQuery(group.id).result.flatMap { r =>
      r.headOption.map { mostRecentId =>
        findWithoutAccessCheckAction(mostRecentId)
      }.getOrElse(DBIO.successful(None))
    }
  }

  def maybeCurrentFor(group: BehaviorGroup): Future[Option[BehaviorGroupVersion]] = {
    dataService.run(maybeCurrentForAction(group))
  }

  def allCurrentIds: Future[Seq[String]] = {
    dataService.run(allCurrentIdsQuery.result)
  }

  def maybeFirstForAction(group: BehaviorGroup): DBIO[Option[BehaviorGroupVersion]] = {
    firstIdForQuery(group.id).result.flatMap { r =>
      r.headOption.map { firstId =>
        findWithoutAccessCheckAction(firstId)
      }.getOrElse(DBIO.successful(None))
    }
  }

  def maybeFirstFor(group: BehaviorGroup): Future[Option[BehaviorGroupVersion]] = {
    dataService.run(maybeFirstForAction(group))
  }

  def createForAction(
                 group: BehaviorGroup,
                 user: User,
                 maybeName: Option[String] = None,
                 maybeIcon: Option[String] = None,
                 maybeDescription: Option[String] = None,
                 maybeGitSHA: Option[String] = None
               ): DBIO[BehaviorGroupVersion] = {
    val newInstance = BehaviorGroupVersion(IDs.next, group, maybeName.getOrElse(""), maybeIcon, maybeDescription, Some(user), OffsetDateTime.now)
    (for {
      _ <- all += newInstance.toRaw
      _ <- maybeGitSHA.map { gitSHA =>
        dataService.behaviorGroupVersionSHAs.createForAction(newInstance, gitSHA)
      }.getOrElse(DBIO.successful({}))
    } yield newInstance).transactionally
  }

  def createFor(
                 group: BehaviorGroup,
                 user: User,
                 maybeName: Option[String] = None,
                 maybeIcon: Option[String] = None,
                 maybeDescription: Option[String] = None,
                 maybeGitSHA: Option[String] = None
               ): Future[BehaviorGroupVersion] = {
    dataService.run(createForAction(group, user, maybeName, maybeIcon, maybeDescription, maybeGitSHA))
  }

  def withoutBuiltin(params: Array[String]) = params.filterNot(ea => ea == services.AWSLambdaConstants.CONTEXT_PARAM)

  def createForBehaviorGroupData(
                                  group: BehaviorGroup,
                                  user: User,
                                  data: BehaviorGroupData
                                ): Future[BehaviorGroupVersion] = {
    val action = (for {
      groupVersion <- createForAction(group, user, data.name, data.icon, data.description, data.gitSHA)
      _ <- DBIO.sequence(data.dataTypeInputs.map { ea =>
        dataService.inputs.ensureForAction(ea, groupVersion)
      })
      awsConfigs <- dataService.awsConfigs.allForAction(group.team)
      requiredAWSConfigs <- DBIO.sequence(data.requiredAWSConfigs.map { requiredData =>
        dataService.requiredAWSConfigs.createForAction(requiredData, groupVersion)
      })
      requiredOAuth1ApiConfigs <- data.requiredOAuth1ApiConfigsAction(dataService).flatMap { requiredConfigs =>
        DBIO.sequence(requiredConfigs.map { ea =>
          dataService.requiredOAuth1ApiConfigs.maybeCreateForAction(ea, groupVersion)
        })
      }.map(_.flatten)
      requiredOAuth2ApiConfigs <- data.requiredOAuth2ApiConfigsAction(dataService).flatMap { requiredConfigs =>
        DBIO.sequence(requiredConfigs.map { ea =>
          dataService.requiredOAuth2ApiConfigs.maybeCreateForAction(ea, groupVersion)
        })
      }.map(_.flatten)
      requiredSimpleTokenApis <- DBIO.sequence(data.requiredSimpleTokenApis.map { requiredData =>
        dataService.requiredSimpleTokenApis.maybeCreateForAction(requiredData, groupVersion)
      }).map(_.flatten)
      _ <- DBIO.sequence(data.libraryVersions.map { ea =>
        dataService.libraries.ensureForAction(ea, groupVersion)
      })
      apiConfig <- DBIO.successful(ApiConfigInfo(awsConfigs, requiredAWSConfigs, requiredOAuth1ApiConfigs, requiredOAuth2ApiConfigs, requiredSimpleTokenApis))
      dataTypeBehaviorVersionTuples <- DBIO.sequence(data.dataTypeBehaviorVersions.map { ea =>
        ea.behaviorId.map { behaviorId =>
          for {
            maybeExistingBehavior <- dataService.behaviors.findAction(behaviorId, user)
            behavior <- maybeExistingBehavior.map(DBIO.successful).getOrElse {
              dataService.behaviors.createForAction(group, Some(behaviorId), ea.exportId, ea.config.isDataType)
            }
            behaviorVersion <- dataService.behaviorVersions.createForAction(behavior, groupVersion, apiConfig, Some(user), ea)
          } yield Some((ea, behaviorVersion))
        }.getOrElse(DBIO.successful(None))
      }).map(_.flatten)
      dataTypeConfigTuples <- DBIO.sequence(dataTypeBehaviorVersionTuples.map { case(data, bv) =>
        dataService.dataTypeConfigs.maybeForAction(bv).map { maybeConfig =>
          maybeConfig.map { config => (data, config) }
        }
      }).map(_.flatten)
      _ <- DBIO.sequence(dataTypeConfigTuples.map { case(data, config) =>
        data.config.dataTypeConfig.map { configData =>
          DBIO.sequence(configData.fields.filterNot(_.isBuiltin).zipWithIndex.map { case (ea, i) =>
            dataService.dataTypeFields.createForAction(ea, i + 1, config, groupVersion)
          })
        }.getOrElse(DBIO.successful(Seq()))
      })
      _ <- DBIO.sequence(data.dataTypeInputs.map { ea =>
        dataService.inputs.ensureForAction(ea, groupVersion)
      })
      _ <- DBIO.sequence(data.actionInputs.map { ea =>
        dataService.inputs.ensureForAction(ea, groupVersion)
      })
      _ <- DBIO.sequence(data.actionBehaviorVersions.map { ea =>
        ea.behaviorId.map { behaviorId =>
          for {
            maybeExistingBehavior <- dataService.behaviors.findAction(behaviorId, user)
            behavior <- maybeExistingBehavior.map(DBIO.successful).getOrElse {
              dataService.behaviors.createForAction(group, Some(behaviorId), ea.exportId, ea.config.isDataType)
            }
            behaviorVersion <- dataService.behaviorVersions.createForAction(behavior, groupVersion, apiConfig, Some(user), ea)
          } yield Some(behaviorVersion)
        }.getOrElse(DBIO.successful(None))
      })
      libraries <- dataService.libraries.allForAction(groupVersion)
      behaviorVersions <- dataService.behaviorVersions.allForGroupVersionAction(groupVersion)
      behaviorVersionsWithParams <- DBIO.sequence(behaviorVersions.map { bv =>
        dataService.behaviorParameters.allForAction(bv).map { params =>
          (bv, params)
        }
      })
      _ <- (for {
        githubRepoData <- LinkedGithubRepoData.maybeFrom(data)
      } yield {
        dataService.linkedGithubRepos.ensureLinkAction(group, githubRepoData.owner, githubRepoData.repo, Some(githubRepoData.currentBranch))
      }).getOrElse(DBIO.successful({}))
    } yield {
      // deploy in the background
      lambdaService.deployFunctionFor(
        groupVersion,
        behaviorVersionsWithParams,
        libraries,
        apiConfig
      )
      groupVersion
    }) transactionally

    dataService.run(action)
  }

  def redeploy(groupVersion: BehaviorGroupVersion): Future[Unit] = {
    for {
      awsConfigs <- dataService.awsConfigs.allFor(groupVersion.team)
      requiredAWSConfigs <- dataService.requiredAWSConfigs.allFor(groupVersion)
      requiredOAuth1ApiConfigs <- dataService.requiredOAuth1ApiConfigs.allFor(groupVersion)
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allFor(groupVersion)
      requiredSimpleTokenApis <- dataService.requiredSimpleTokenApis.allFor(groupVersion)
      apiConfig <- Future.successful(ApiConfigInfo(awsConfigs, requiredAWSConfigs, requiredOAuth1ApiConfigs, requiredOAuth2ApiConfigs, requiredSimpleTokenApis))
      libraries <- dataService.libraries.allFor(groupVersion)
      behaviorVersions <- dataService.behaviorVersions.allForGroupVersion(groupVersion)
      behaviorVersionsWithParams <- Future.sequence(behaviorVersions.map { bv =>
        dataService.behaviorParameters.allFor(bv).map { params => (bv, params) }
      })
      _ <- lambdaService.deployFunctionFor(
        groupVersion,
        behaviorVersionsWithParams,
        libraries,
        apiConfig
      )
      _ <- dataService.run(lambdaService.ensureNodeModuleVersionsFor(groupVersion))
    } yield {}
  }

  private def redeployAllSequentially(versions: Seq[BehaviorGroupVersion]): Future[Unit] = {
    versions.headOption.map { v =>
      redeploy(v).recover {
        case e: Exception => {
          Logger.info(s"Error redeploying version with ID: ${v.id}: ${e.getMessage}")
        }
      }.flatMap { _ =>
        redeployAllSequentially(versions.tail)
      }
    }.getOrElse(Future.successful(Unit))
  }

  def redeployAllCurrentVersions: Future[Unit] = {
    allCurrent.map { current =>
      for (version <- current) {
        Await.ready(redeploy(version).recover {
          case e: Exception => {
            Logger.info(s"Error redeploying version with ID: ${version.id}: ${e.getMessage}")
          }
        }, 30.seconds)
      }
    }
  }

  private def allCurrent: Future[Seq[BehaviorGroupVersion]] = {
    val action = allCurrentQuery.result.map { r =>
      r.map(tuple2BehaviorGroupVersion)
    }
    dataService.run(action)
  }

  private def currentFunctionNames: Future[Seq[String]] = {
    dataService.run(allCurrentIdsQuery.result).map { r =>
      r.map(BehaviorGroupVersion.functionNameFor)
    }
  }

  private def activeConversationFunctionNames: Future[Seq[String]] = {
    dataService.conversations.allOngoingBehaviorGroupVersionIds.map { ids =>
      ids.map(BehaviorGroupVersion.functionNameFor)
    }
  }

  private def deployedFunctionNames: Future[Seq[String]] = {
    dataService.behaviorGroupDeployments.mostRecentBehaviorGroupVersionIds.map { ids =>
      ids.map(BehaviorGroupVersion.functionNameFor)
    }
  }

  def haveActionsWithNameAndSameInterface(
                                           actionName: String,
                                           groupVersion1: BehaviorGroupVersion,
                                           groupVersion2: BehaviorGroupVersion
                                         ): Future[Boolean] = {
    for {
      maybeBehaviorVersion <- dataService.behaviorVersions.findByName(actionName, groupVersion1)
      maybeActiveBehaviorVersion <- dataService.behaviorVersions.findByName(actionName, groupVersion2)
      behaviorVersionsMatch <- (for {
        behaviorVersion <- maybeBehaviorVersion
        activeBehaviorVersion <- maybeActiveBehaviorVersion
      } yield {
        dataService.behaviorVersions.haveSameInterface(behaviorVersion, activeBehaviorVersion)
      }).getOrElse(Future.successful(false))
    } yield behaviorVersionsMatch
  }

  def isActive(groupVersion: BehaviorGroupVersion, context: String, channel: String): Future[Boolean] = {
    dataService.behaviorGroupDeployments.maybeActiveBehaviorGroupVersionFor(groupVersion.group, context, channel).map{ maybeActive =>
      maybeActive.contains(groupVersion)
    }
  }

  def activeFunctionNames: Future[Seq[String]] = {
    for {
      current <- currentFunctionNames
      deployed <- deployedFunctionNames
      activeConvo <- activeConversationFunctionNames
    } yield {
      (current ++ deployed ++ activeConvo).distinct
    }
  }

  def hasNewerVersionForAuthorAction(version: BehaviorGroupVersion, user: User): DBIO[Boolean] = {
    newerVersionsForAuthorQuery(version.group.id, version.createdAt, user.id).result.map(_.nonEmpty)
  }

}
