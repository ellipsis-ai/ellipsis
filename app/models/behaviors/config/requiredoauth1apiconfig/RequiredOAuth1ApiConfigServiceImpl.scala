package models.behaviors.config.requiredoauth1apiconfig

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import json.RequiredOAuth1ApiConfigData
import models.IDs
import models.accounts.oauth1api.{OAuth1Api, OAuth1ApiQueries}
import models.accounts.oauth1application.OAuth1ApplicationQueries
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.{BehaviorGroupVersion, BehaviorGroupVersionQueries}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class RawRequiredOAuth1ApiConfig(
                                       id: String,
                                       exportId: String,
                                       groupVersionId: String,
                                       apiId: String,
                                       nameInCode: String,
                                       maybeApplicationId: Option[String]
                                     )

class RequiredOAuth1ApiConfigsTable(tag: Tag) extends Table[RawRequiredOAuth1ApiConfig](tag, "required_oauth1_api_configs") {

  def id = column[String]("id", O.PrimaryKey)
  def exportId = column[String]("export_id")
  def groupVersionId = column[String]("group_version_id")
  def apiId = column[String]("api_id")
  def nameInCode = column[String]("name_in_code")
  def maybeApplicationId = column[Option[String]]("application_id")

  def * = (id, exportId, groupVersionId, apiId, nameInCode, maybeApplicationId) <> ((RawRequiredOAuth1ApiConfig.apply _).tupled, RawRequiredOAuth1ApiConfig.unapply _)
}

class RequiredOAuth1ApiConfigServiceImpl @Inject() (
                                                     dataServiceProvider: Provider[DataService],
                                                     implicit val ec: ExecutionContext
                                                   ) extends RequiredOAuth1ApiConfigService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[RequiredOAuth1ApiConfigsTable]
  val allWithGroupVersion = all.join(BehaviorGroupVersionQueries.allWithUser).on(_.groupVersionId === _._1._1.id)
  val allWithApi = allWithGroupVersion.join(OAuth1ApiQueries.all).on(_._1.apiId === _.id)
  val allWithApplication = allWithApi.joinLeft(OAuth1ApplicationQueries.allWithApi).on(_._1._1.maybeApplicationId === _._1.id)

  type TupleType = (((RawRequiredOAuth1ApiConfig, BehaviorGroupVersionQueries.TupleType), OAuth1Api), Option[(OAuth1ApplicationQueries.TupleType)])

  def tuple2Required(tuple: TupleType): RequiredOAuth1ApiConfig = {
    val raw = tuple._1._1._1
    val groupVersion = BehaviorGroupVersionQueries.tuple2BehaviorGroupVersion(tuple._1._1._2)
    RequiredOAuth1ApiConfig(
      raw.id,
      raw.exportId,
      groupVersion,
      tuple._1._2,
      raw.nameInCode,
      tuple._2.map(OAuth1ApplicationQueries.tuple2Application)
    )
  }

  def uncompiledAllForQuery(groupVersionId: Rep[String]) = {
    allWithApplication.
      filter { case(((required, _), _), _) => required.groupVersionId === groupVersionId }.
      sortBy { case(((required, _), _), _) => required.nameInCode }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allForIdAction(groupVersionId: String): DBIO[Seq[RequiredOAuth1ApiConfig]] = {
    allForQuery(groupVersionId).result.map(r => r.map(tuple2Required))
  }

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[RequiredOAuth1ApiConfig]] = {
    allForIdAction(groupVersion.id)
  }

  def allForId(groupVersionId: String): Future[Seq[RequiredOAuth1ApiConfig]] = {
    dataService.run(allForIdAction(groupVersionId))
  }

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[RequiredOAuth1ApiConfig]] = {
    dataService.run(allForAction(groupVersion))
  }

  def allFor(api: OAuth1Api, behaviorGroup: BehaviorGroup): Future[Seq[RequiredOAuth1ApiConfig]] = {
    for {
      maybeCurrentVersion <- dataService.behaviorGroups.maybeCurrentVersionFor(behaviorGroup)
      required <- maybeCurrentVersion.map { currentVersion =>
        allFor(api, currentVersion)
      }.getOrElse(Future.successful(Seq()))
    } yield required
  }

  def uncompiledAllForApiAndVersionQuery(apiId: Rep[String], groupVersionId: Rep[String]) = {
    allWithApplication.filter(_._1._1._1.apiId === apiId).filter(_._1._1._1.groupVersionId === groupVersionId)
  }
  val allForApiAndVersionQuery = Compiled(uncompiledAllForApiAndVersionQuery _)

  def allFor(api: OAuth1Api, groupVersion: BehaviorGroupVersion): Future[Seq[RequiredOAuth1ApiConfig]] = {
    val action = allForApiAndVersionQuery(api.id, groupVersion.id).result.map { r =>
      r.map(tuple2Required)
    }
    dataService.run(action)
  }

  def uncompiledFindQuery(id: Rep[String]) = allWithApplication.filter(_._1._1._1.id === id)
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(id: String): Future[Option[RequiredOAuth1ApiConfig]] = {
    val action = findQuery(id).result.map { r =>
      r.headOption.map(tuple2Required)
    }
    dataService.run(action)
  }

  def uncompiledFindWithNameInCodeQuery(nameInCode: Rep[String], groupVersionId: Rep[String]) = {
    allWithApplication.
      filter(_._1._1._1.nameInCode === nameInCode).
      filter(_._1._1._1.groupVersionId === groupVersionId)
  }
  val findWithNameInCodeQuery = Compiled(uncompiledFindWithNameInCodeQuery _)

  def findWithNameInCode(nameInCode: String, groupVersion: BehaviorGroupVersion): Future[Option[RequiredOAuth1ApiConfig]] = {
    val action = findWithNameInCodeQuery(nameInCode, groupVersion.id).result.map { r =>
      r.headOption.map(tuple2Required)
    }
    dataService.run(action)
  }

  def uncompiledFindRawQuery(id: Rep[String]) = all.filter(_.id === id)
  val findRawQuery = Compiled(uncompiledFindRawQuery _)

  def save(requiredOAuth1ApiConfig: RequiredOAuth1ApiConfig): Future[RequiredOAuth1ApiConfig] = {
    val query = findRawQuery(requiredOAuth1ApiConfig.id)
    val raw = requiredOAuth1ApiConfig.toRaw
    val action = query.result.flatMap { r =>
      r.headOption.map { _ =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map(_ => requiredOAuth1ApiConfig)
    dataService.run(action)
  }

  def maybeCreateForAction(data: RequiredOAuth1ApiConfigData, groupVersion: BehaviorGroupVersion): DBIO[Option[RequiredOAuth1ApiConfig]] = {
    for {
      maybeApi <- DBIO.from(dataService.oauth1Apis.find(data.apiId))
      maybeApplication <- data.config.map { appData =>
        DBIO.from(dataService.oauth1Applications.find(appData.id))
      }.getOrElse(DBIO.successful(None))
      maybeConfig <- maybeApi.map { api =>
        val newInstance = RequiredOAuth1ApiConfig(IDs.next, data.exportId.getOrElse(IDs.next), groupVersion, api, data.nameInCode, maybeApplication)
        (all += newInstance.toRaw).map(_ => newInstance).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield maybeConfig
  }

  def maybeCreateFor(data: RequiredOAuth1ApiConfigData, groupVersion: BehaviorGroupVersion): Future[Option[RequiredOAuth1ApiConfig]] = {
    dataService.run(maybeCreateForAction(data, groupVersion))
  }

}
