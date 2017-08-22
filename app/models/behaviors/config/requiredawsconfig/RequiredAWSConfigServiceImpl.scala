package models.behaviors.config.requiredawsconfig

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.RequiredAWSConfigData
import models.IDs
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.{BehaviorGroupVersion, BehaviorGroupVersionQueries}
import models.behaviors.config.awsconfig.{AWSConfig, AWSConfigQueries}
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawRequiredAWSConfig(
                                 id: String,
                                 groupVersionId: String,
                                 maybeConfigId: Option[String]
                               )

class RequiredAWSConfigsTable(tag: Tag) extends Table[RawRequiredAWSConfig](tag, "required_aws_configs") {

  def id = column[String]("id", O.PrimaryKey)
  def groupVersionId = column[String]("group_version_id")
  def maybeConfigId = column[Option[String]]("config_id")

  def * = (id, groupVersionId, maybeConfigId) <> ((RawRequiredAWSConfig.apply _).tupled, RawRequiredAWSConfig.unapply _)
}

class RequiredAWSConfigServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService]
                                             ) extends RequiredAWSConfigService {

  def dataService: DataService = dataServiceProvider.get

  val all = TableQuery[RequiredAWSConfigsTable]
  val allWithGroupVersion = all.join(BehaviorGroupVersionQueries.allWithUser).on(_.groupVersionId === _._1._1.id)
  val allWithAWSConfig = allWithGroupVersion.joinLeft(AWSConfigQueries.all).on(_._1.maybeConfigId === _.id)

  type TupleType = ((RawRequiredAWSConfig, BehaviorGroupVersionQueries.TupleType), Option[AWSConfig])

  def tuple2RequiredAWSConfig(tuple: TupleType): RequiredAWSConfig = {
    val raw = tuple._1._1
    val groupVersion = BehaviorGroupVersionQueries.tuple2BehaviorGroupVersion(tuple._1._2)
    val maybeConfig = tuple._2
    RequiredAWSConfig(
      raw.id,
      groupVersion,
      maybeConfig
    )
  }

  def uncompiledAllForQuery(groupVersionId: Rep[String]) = {
    allWithAWSConfig.filter { case((_, ((groupVersion, _), _)), _) => groupVersion.id === groupVersionId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[RequiredAWSConfig]] = {
    val action = allForQuery(groupVersion.id).result.map { r =>
      r.map(tuple2RequiredAWSConfig)
    }
    dataService.run(action)
  }

  def allFor(group: BehaviorGroup): Future[Seq[RequiredAWSConfig]] = {
    for {
      maybeCurrentVersion <- dataService.behaviorGroups.maybeCurrentVersionFor(group)
      required <- maybeCurrentVersion.map { currentVersion =>
        allFor(currentVersion)
      }.getOrElse(Future.successful(Seq()))
    } yield required
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithAWSConfig.filter { case ((required, _), _) => required.id === id }
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
      maybeConfig <- data.config.map { configData =>
        dataService.awsConfigs.findAction(configData.id)
      }.getOrElse(DBIO.successful(None))
      required <- {
        val newInstance = RequiredAWSConfig(IDs.next, groupVersion, maybeConfig)
        (all += newInstance.toRaw).map(_ => newInstance)
      }
    } yield required
  }

}
