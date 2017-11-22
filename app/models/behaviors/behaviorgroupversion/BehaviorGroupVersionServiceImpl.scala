package models.behaviors.behaviorgroupversion

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.BehaviorGroupData
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroup.{BehaviorGroup, BehaviorGroupQueries}
import play.api.Logger
import services.{AWSLambdaService, ApiConfigInfo, DataService}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

case class RawBehaviorGroupVersion(
                                   id: String,
                                   groupId: String,
                                   name: String,
                                   maybeIcon: Option[String],
                                   maybeDescription: Option[String],
                                   maybeAuthorId: Option[String],
                                   maybeGitSHA: Option[String],
                                   createdAt: OffsetDateTime
                                 )

class BehaviorGroupVersionsTable(tag: Tag) extends Table[RawBehaviorGroupVersion](tag, "behavior_group_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def groupId = column[String]("group_id")
  def name = column[String]("name")
  def maybeIcon = column[Option[String]]("icon")
  def maybeDescription = column[Option[String]]("description")
  def maybeAuthorId = column[Option[String]]("author_id")
  def maybeGitSHA = column[Option[String]]("git_sha")
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, groupId, name, maybeIcon, maybeDescription, maybeAuthorId, maybeGitSHA, createdAt) <>
      ((RawBehaviorGroupVersion.apply _).tupled, RawBehaviorGroupVersion.unapply _)
}

class BehaviorGroupVersionServiceImpl @Inject() (
                                                   dataServiceProvider: Provider[DataService],
                                                   lambdaServiceProvider: Provider[AWSLambdaService],
                                                   implicit val ec: ExecutionContext
                                                ) extends BehaviorGroupVersionService {

  def dataService = dataServiceProvider.get
  def lambdaService = lambdaServiceProvider.get

  import BehaviorGroupVersionQueries._

  def findWithoutAccessCheckAction(id: String): DBIO[Option[BehaviorGroupVersion]] = {
    findQuery(id).result.map(_.headOption.map(tuple2BehaviorGroupVersion))
  }

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorGroupVersion]] = {
    dataService.run(findWithoutAccessCheckAction(id))
  }

  def allFor(group: BehaviorGroup): Future[Seq[BehaviorGroupVersion]] = {
    val action = allForQuery(group.id).result.map { r =>
      r.map(tuple2BehaviorGroupVersion)
    }
    dataService.run(action)
  }

  def createForAction(
                 group: BehaviorGroup,
                 user: User,
                 maybeName: Option[String] = None,
                 maybeIcon: Option[String] = None,
                 maybeDescription: Option[String] = None,
                 maybeGitSHA: Option[String] = None
               ): DBIO[BehaviorGroupVersion] = {
    val raw = RawBehaviorGroupVersion(IDs.next, group.id, maybeName.getOrElse(""), maybeIcon, maybeDescription, Some(user.id), maybeGitSHA, OffsetDateTime.now)

    (all += raw).flatMap { _ =>
      BehaviorGroupQueries.findQuery(group.id).result.map { r =>
        val reloadedGroup = r.headOption.map(BehaviorGroupQueries.tuple2Group).get // must exist; reload so it has current versionid
        BehaviorGroupVersion(raw.id, reloadedGroup, raw.name, raw.maybeIcon, raw.maybeDescription, Some(user), raw.maybeGitSHA, raw.createdAt)
      }
    }
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

  def createFor(
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
      requiredOAuth2ApiConfigs <- DBIO.sequence(data.requiredOAuth2ApiConfigs.map { requiredData =>
        dataService.requiredOAuth2ApiConfigs.maybeCreateForAction(requiredData, groupVersion)
      }).map(_.flatten)
      requiredSimpleTokenApis <- DBIO.sequence(data.requiredSimpleTokenApis.map { requiredData =>
        dataService.requiredSimpleTokenApis.maybeCreateForAction(requiredData, groupVersion)
      }).map(_.flatten)
      _ <- DBIO.sequence(data.libraryVersions.map { ea =>
        dataService.libraries.ensureForAction(ea, groupVersion)
      })
      apiConfig <- DBIO.successful(ApiConfigInfo(awsConfigs, requiredAWSConfigs, requiredOAuth2ApiConfigs, requiredSimpleTokenApis))
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
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allFor(groupVersion)
      requiredSimpleTokenApis <- dataService.requiredSimpleTokenApis.allFor(groupVersion)
      apiConfig <- Future.successful(ApiConfigInfo(awsConfigs, requiredAWSConfigs, requiredOAuth2ApiConfigs, requiredSimpleTokenApis))
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
    allCurrent.map { currentVersions =>
      for (v <- currentVersions) {
        Await.ready(redeploy(v).recover {
          case e: Exception => {
            Logger.info(s"Error redeploying version with ID: ${v.id}: ${e.getMessage}")
          }
        }, 30.seconds)
      }
    }
  }

  def uncompiledAllCurrentQuery = {
    allWithUser.filter {
      case ((groupVersion, (group, _)), _) => groupVersion.id === group.maybeCurrentVersionId
    }
  }
  val allCurrentQuery = Compiled(uncompiledAllCurrentQuery)

  def uncompiledAllCurrentIdsQuery() = {
    uncompiledAllCurrentQuery.map {
      case ((groupVersion, _), _) => groupVersion.id
    }
  }
  val allCurrentIdsQuery = Compiled(uncompiledAllCurrentIdsQuery)

  private def allCurrent: Future[Seq[BehaviorGroupVersion]] = {
    val action = allCurrentQuery.result.map { r =>
      r.map(tuple2BehaviorGroupVersion)
    }
    dataService.run(action)
  }

  def maybePreviousFor(groupVersion: BehaviorGroupVersion): Future[Option[BehaviorGroupVersion]] = {
    allFor(groupVersion.group).map { versions =>
      val index = versions.indexWhere(_.id == groupVersion.id)
      if (index == versions.size - 1) {
        None
      } else {
        Some(versions(index + 1))
      }
    }
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

  def activeFunctionNames: Future[Seq[String]] = {
    for {
      current <- currentFunctionNames
      activeConvo <- activeConversationFunctionNames
    } yield {
      (current ++ activeConvo).distinct
    }
  }

}
