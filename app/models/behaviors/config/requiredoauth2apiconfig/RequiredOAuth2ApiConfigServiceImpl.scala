package models.behaviors.config.requiredoauth2apiconfig

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.RequiredOAuth2ApiConfigData
import models.IDs
import models.accounts.oauth2api.{OAuth2Api, OAuth2ApiQueries}
import models.accounts.oauth2application.OAuth2ApplicationQueries
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.{BehaviorGroupVersion, BehaviorGroupVersionQueries}
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawRequiredOAuth2ApiConfig(
                                       id: String,
                                       groupVersionId: String,
                                       apiId: String,
                                       maybeRecommendedScope: Option[String],
                                       maybeApplicationId: Option[String]
                                     )

class RequiredOAuth2ApiConfigsTable(tag: Tag) extends Table[RawRequiredOAuth2ApiConfig](tag, "required_oauth2_api_configs") {

  def id = column[String]("id", O.PrimaryKey)
  def groupVersionId = column[String]("group_version_id")
  def apiId = column[String]("api_id")
  def maybeRecommendedScope = column[Option[String]]("recommended_scope")
  def maybeApplicationId = column[Option[String]]("application_id")

  def * = (id, groupVersionId, apiId, maybeRecommendedScope, maybeApplicationId) <> ((RawRequiredOAuth2ApiConfig.apply _).tupled, RawRequiredOAuth2ApiConfig.unapply _)
}

class RequiredOAuth2ApiConfigServiceImpl @Inject() (
                                                     dataServiceProvider: Provider[DataService]
                                                   ) extends RequiredOAuth2ApiConfigService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[RequiredOAuth2ApiConfigsTable]
  val allWithGroupVersion = all.join(BehaviorGroupVersionQueries.allWithUser).on(_.groupVersionId === _._1._1.id)
  val allWithApi = allWithGroupVersion.join(OAuth2ApiQueries.all).on(_._1.apiId === _.id)
  val allWithApplication = allWithApi.joinLeft(OAuth2ApplicationQueries.allWithApi).on(_._1._1.maybeApplicationId === _._1.id)

  type TupleType = (((RawRequiredOAuth2ApiConfig, BehaviorGroupVersionQueries.TupleType), OAuth2Api), Option[(OAuth2ApplicationQueries.TupleType)])

  def tuple2Required(tuple: TupleType): RequiredOAuth2ApiConfig = {
    val raw = tuple._1._1._1
    val groupVersion = BehaviorGroupVersionQueries.tuple2BehaviorGroupVersion(tuple._1._1._2)
    RequiredOAuth2ApiConfig(
      raw.id,
      groupVersion,
      tuple._1._2,
      raw.maybeRecommendedScope,
      tuple._2.map(OAuth2ApplicationQueries.tuple2Application)
    )
  }

  def uncompiledAllForQuery(groupVersionId: Rep[String]) = {
    allWithApplication.filter { case(((required, _), _), _) => required.groupVersionId === groupVersionId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[RequiredOAuth2ApiConfig]] = {
    val action = allForQuery(groupVersion.id).result.map(r => r.map(tuple2Required))
    dataService.run(action)
  }

  def allFor(api: OAuth2Api, behaviorGroup: BehaviorGroup): Future[Seq[RequiredOAuth2ApiConfig]] = {
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

  def allFor(api: OAuth2Api, groupVersion: BehaviorGroupVersion): Future[Seq[RequiredOAuth2ApiConfig]] = {
    val action = allForApiAndVersionQuery(api.id, groupVersion.id).result.map { r =>
      r.map(tuple2Required)
    }
    dataService.run(action)
  }

  def uncompiledFindQuery(id: Rep[String]) = allWithApplication.filter(_._1._1._1.id === id)
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(id: String): Future[Option[RequiredOAuth2ApiConfig]] = {
    val action = findQuery(id).result.map { r =>
      r.headOption.map(tuple2Required)
    }
    dataService.run(action)
  }

  def uncompiledFindRawQuery(id: Rep[String]) = all.filter(_.id === id)
  val findRawQuery = Compiled(uncompiledFindRawQuery _)

  def save(requiredOAuth2ApiConfig: RequiredOAuth2ApiConfig): Future[RequiredOAuth2ApiConfig] = {
    val query = findRawQuery(requiredOAuth2ApiConfig.id)
    val raw = requiredOAuth2ApiConfig.toRaw
    val action = query.result.flatMap { r =>
      r.headOption.map { _ =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map(_ => requiredOAuth2ApiConfig)
    dataService.run(action)
  }

  def maybeCreateForAction(data: RequiredOAuth2ApiConfigData, groupVersion: BehaviorGroupVersion): DBIO[Option[RequiredOAuth2ApiConfig]] = {
    for {
      maybeApi <- DBIO.from(dataService.oauth2Apis.find(data.apiId))
      maybeApplication <- data.application.map { appData =>
        DBIO.from(dataService.oauth2Applications.find(appData.applicationId))
      }.getOrElse(DBIO.successful(None))
      maybeConfig <- maybeApi.map { api =>
        val newInstance = RequiredOAuth2ApiConfig(IDs.next, groupVersion, api, data.recommendedScope, maybeApplication)
        (all += newInstance.toRaw).map(_ => newInstance).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield maybeConfig
  }

}
