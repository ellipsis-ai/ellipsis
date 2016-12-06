package models.behaviors.config.requiredoauth2apiconfig

import javax.inject.Inject

import com.google.inject.Provider
import json.RequiredOAuth2ApiConfigData
import models.IDs
import models.accounts.oauth2api.{OAuth2Api, OAuth2ApiQueries}
import models.accounts.oauth2application.OAuth2ApplicationQueries
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorversion.{BehaviorVersion, BehaviorVersionQueries}
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawRequiredOAuth2ApiConfig(
                                       id: String,
                                       behaviorVersionId: String,
                                       apiId: String,
                                       maybeRecommendedScope: Option[String],
                                       maybeApplicationId: Option[String]
                                     )

class RequiredOAuth2ApiConfigsTable(tag: Tag) extends Table[RawRequiredOAuth2ApiConfig](tag, "required_oauth2_api_configs") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def apiId = column[String]("api_id")
  def maybeRecommendedScope = column[Option[String]]("recommended_scope")
  def maybeApplicationId = column[Option[String]]("application_id")

  def * = (id, behaviorVersionId, apiId, maybeRecommendedScope, maybeApplicationId) <> ((RawRequiredOAuth2ApiConfig.apply _).tupled, RawRequiredOAuth2ApiConfig.unapply _)
}

class RequiredOAuth2ApiConfigServiceImpl @Inject() (
                                                     dataServiceProvider: Provider[DataService]
                                                   ) extends RequiredOAuth2ApiConfigService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[RequiredOAuth2ApiConfigsTable]
  val allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithBehavior).on(_.behaviorVersionId === _._1._1.id)
  val allWithApi = allWithBehaviorVersion.join(OAuth2ApiQueries.all).on(_._1.apiId === _.id)
  val allWithApplication = allWithApi.joinLeft(OAuth2ApplicationQueries.allWithApi).on(_._1._1.maybeApplicationId === _._1.id)

  type TupleType = (((RawRequiredOAuth2ApiConfig, BehaviorVersionQueries.TupleType), OAuth2Api), Option[(OAuth2ApplicationQueries.TupleType)])

  def tuple2Required(tuple: TupleType): RequiredOAuth2ApiConfig = {
    val raw = tuple._1._1._1
    val behaviorVersion = BehaviorVersionQueries.tuple2BehaviorVersion(tuple._1._1._2)
    RequiredOAuth2ApiConfig(
      raw.id,
      behaviorVersion,
      tuple._1._2,
      raw.maybeRecommendedScope,
      tuple._2.map(OAuth2ApplicationQueries.tuple2Application)
    )
  }

  def uncompiledAllForQuery(behaviorVersionId: Rep[String]) = {
    allWithApplication.filter { case(((required, _), _), _) => required.behaviorVersionId === behaviorVersionId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[RequiredOAuth2ApiConfig]] = {
    val action = allForQuery(behaviorVersion.id).result.map(r => r.map(tuple2Required))
    dataService.run(action)
  }

  def allFor(api: OAuth2Api, behaviorGroup: BehaviorGroup): Future[Seq[RequiredOAuth2ApiConfig]] = {
    for {
      behaviors <- dataService.behaviors.allForGroup(behaviorGroup)
      versions <- Future.sequence(behaviors.map { ea =>
        dataService.behaviors.maybeCurrentVersionFor(ea)
      }).map(_.flatten)
      required <- Future.sequence(versions.map { ea =>
        allFor(api, ea)
      }).map(_.flatten)
    } yield required
  }

  def uncompiledAllForApiAndVersionQuery(apiId: Rep[String], behaviorVersionId: Rep[String]) = {
    allWithApplication.filter(_._1._1._1.apiId === apiId).filter(_._1._1._1.behaviorVersionId === behaviorVersionId)
  }
  val allForApiAndVersionQuery = Compiled(uncompiledAllForApiAndVersionQuery _)

  def allFor(api: OAuth2Api, behaviorVersion: BehaviorVersion): Future[Seq[RequiredOAuth2ApiConfig]] = {
    val action = allForApiAndVersionQuery(api.id, behaviorVersion.id).result.map { r =>
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

  def maybeCreateFor(data: RequiredOAuth2ApiConfigData, behaviorVersion: BehaviorVersion): Future[Option[RequiredOAuth2ApiConfig]] = {
    val action = for {
      maybeApi <- DBIO.from(dataService.oauth2Apis.find(data.apiId))
      maybeApplication <- data.application.map { appData =>
        DBIO.from(dataService.oauth2Applications.find(appData.applicationId))
      }.getOrElse(DBIO.successful(None))
      maybeConfig <- maybeApi.map { api =>
        val newInstance = RequiredOAuth2ApiConfig(IDs.next, behaviorVersion, api, data.recommendedScope, maybeApplication)
        (all += newInstance.toRaw).map(_ => newInstance).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield maybeConfig

    dataService.run(action)
  }
}
