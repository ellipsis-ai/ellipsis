package models.behaviors.behaviorgroupversion

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.BehaviorGroupData
import models.IDs
import models.accounts.user.User
import models.behaviors.behaviorgroup.{BehaviorGroup, BehaviorGroupQueries}
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
                                             dataServiceProvider: Provider[DataService]
                                           ) extends BehaviorGroupVersionService {

  def dataService = dataServiceProvider.get

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
                 maybeDescription: Option[String] = None
               ): DBIO[BehaviorGroupVersion] = {
    val raw = RawBehaviorGroupVersion(IDs.next, group.id, maybeName.getOrElse(""), maybeIcon, maybeDescription, Some(user.id), OffsetDateTime.now)

    (all += raw).flatMap { _ =>
      BehaviorGroupQueries.findQuery(group.id).result.map { r =>
        val reloadedGroup = r.headOption.map(BehaviorGroupQueries.tuple2Group).get // must exist; reload so it has current versionid
        BehaviorGroupVersion(raw.id, reloadedGroup, raw.name, raw.maybeIcon, raw.maybeDescription, Some(user), raw.createdAt)
      }
    }
  }

  def createFor(
                 group: BehaviorGroup,
                 user: User,
                 maybeName: Option[String] = None,
                 maybeIcon: Option[String] = None,
                 maybeDescription: Option[String] = None
               ): Future[BehaviorGroupVersion] = {
    dataService.run(createForAction(group, user, maybeName, maybeIcon, maybeDescription))
  }

  def createFor(
                 group: BehaviorGroup,
                 user: User,
                 data: BehaviorGroupData
               ): Future[BehaviorGroupVersion] = {
    val action = (for {
      groupVersion <- createForAction(group, user, data.name, data.icon, data.description)
      _ <- DBIO.sequence(data.dataTypeInputs.map { ea =>
        dataService.inputs.ensureForAction(ea, groupVersion)
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
      dataTypeBehaviorVersionTuples <- DBIO.sequence(data.dataTypeBehaviorVersions.map { ea =>
        ea.behaviorId.map { behaviorId =>
          for {
            maybeExistingBehavior <- dataService.behaviors.findAction(behaviorId, user)
            behavior <- maybeExistingBehavior.map(DBIO.successful).getOrElse {
              dataService.behaviors.createForAction(group, Some(behaviorId), ea.exportId, ea.config.isDataType)
            }
            behaviorVersion <- dataService.behaviorVersions.createForAction(behavior, groupVersion, requiredOAuth2ApiConfigs, requiredSimpleTokenApis, Some(user), ea)
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
            behaviorVersion <- dataService.behaviorVersions.createForAction(behavior, groupVersion, requiredOAuth2ApiConfigs, requiredSimpleTokenApis, Some(user), ea)
          } yield Some(behaviorVersion)
        }.getOrElse(DBIO.successful(None))
      })
    } yield groupVersion) transactionally

    dataService.run(action)
  }

}
