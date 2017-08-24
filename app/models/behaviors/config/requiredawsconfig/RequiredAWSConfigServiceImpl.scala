package models.behaviors.config.requiredawsconfig

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.RequiredAWSConfigData
import models.IDs
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.{BehaviorGroupVersion, BehaviorGroupVersionQueries}
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawRequiredAWSConfig(
                                 id: String,
                                 nameInCode: String,
                                 groupVersionId: String
                               )

class RequiredAWSConfigsTable(tag: Tag) extends Table[RawRequiredAWSConfig](tag, "required_aws_configs") {

  def id = column[String]("id", O.PrimaryKey)
  def nameInCode = column[String]("name_in_code")
  def groupVersionId = column[String]("group_version_id")

  def * = (id, nameInCode, groupVersionId) <> ((RawRequiredAWSConfig.apply _).tupled, RawRequiredAWSConfig.unapply _)
}

class RequiredAWSConfigServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService]
                                             ) extends RequiredAWSConfigService {

  def dataService: DataService = dataServiceProvider.get

  val all = TableQuery[RequiredAWSConfigsTable]
  val allWithGroupVersion = all.join(BehaviorGroupVersionQueries.allWithUser).on(_.groupVersionId === _._1._1.id)

  type TupleType = (RawRequiredAWSConfig, BehaviorGroupVersionQueries.TupleType)

  def tuple2RequiredAWSConfig(tuple: TupleType): RequiredAWSConfig = {
    val raw = tuple._1
    val groupVersion = BehaviorGroupVersionQueries.tuple2BehaviorGroupVersion(tuple._2)
    RequiredAWSConfig(
      raw.id,
      raw.nameInCode,
      groupVersion
    )
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithGroupVersion.filter { case(required, _) => required.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(id: String): Future[Option[RequiredAWSConfig]] = {
    val action = findQuery(id).result.map { r =>
      r.headOption.map(tuple2RequiredAWSConfig)
    }
    dataService.run(action)
  }

  def uncompiledAllForQuery(groupVersionId: Rep[String]) = {
    allWithGroupVersion.filter { case(_, ((groupVersion, _), _)) => groupVersion.id === groupVersionId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[RequiredAWSConfig]] = {
    allForQuery(groupVersion.id).result.map { r =>
      r.map(tuple2RequiredAWSConfig)
    }
  }

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[RequiredAWSConfig]] = {
    dataService.run(allForAction(groupVersion))
  }

  def allFor(group: BehaviorGroup): Future[Seq[RequiredAWSConfig]] = {
    for {
      maybeCurrentVersion <- dataService.behaviorGroups.maybeCurrentVersionFor(group)
      required <- maybeCurrentVersion.map { currentVersion =>
        allFor(currentVersion)
      }.getOrElse(Future.successful(Seq()))
    } yield required
  }

  def uncompiledRawFindQuery(id: Rep[String]) = all.filter(_.id === id)
  val rawFindQuery = Compiled(uncompiledRawFindQuery _)

  def save(requiredConfig: RequiredAWSConfig): Future[RequiredAWSConfig] = {
    val action = rawFindQuery(requiredConfig.id).result.map(_.headOption).flatMap { maybeExisting =>
      maybeExisting.map { _ =>
        rawFindQuery(requiredConfig.id).update(requiredConfig.toRaw)
      }.getOrElse {
        (all += requiredConfig.toRaw)
      }
    }.map(_ => requiredConfig)
    dataService.run(action)
  }

  def createForAction(data: RequiredAWSConfigData, groupVersion: BehaviorGroupVersion): DBIO[RequiredAWSConfig] = {
    for {
      required <- {
        val newInstance = RequiredAWSConfig(IDs.next, data.nameInCode, groupVersion)
        (all += newInstance.toRaw).map(_ => newInstance)
      }
    } yield required
  }

}
