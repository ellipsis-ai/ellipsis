package models.bots.config

import json.RequiredOAuth2ApiConfigData
import models.IDs
import models.accounts.{OAuth2Api, OAuth2ApiQueries, OAuth2Application, OAuth2ApplicationQueries}
import models.bots.{BehaviorVersion, BehaviorVersionQueries}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class RequiredOAuth2ApiConfig(
                                    id: String,
                                    behaviorVersion: BehaviorVersion,
                                    api: OAuth2Api,
                                    maybeRecommendedScope: Option[String],
                                    maybeApplication: Option[OAuth2Application]
                                    ) {
  // Could check scope too
  def isReady: Boolean = maybeApplication.isDefined

  def toRaw: RawRequiredOAuth2ApiConfig = {
    RawRequiredOAuth2ApiConfig(
      id,
      behaviorVersion.id,
      api.id,
      maybeRecommendedScope,
      maybeApplication.map(_.id)
    )
  }

}

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

object RequiredOAuth2ApiConfigQueries {
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

  def allFor(behaviorVersion: BehaviorVersion): DBIO[Seq[RequiredOAuth2ApiConfig]] = {
    allForQuery(behaviorVersion.id).result.map(r => r.map(tuple2Required))
  }

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 api: OAuth2Api,
                 maybeRecommendedScope: Option[String],
                 maybeApplication: Option[OAuth2Application]
               ): DBIO[RequiredOAuth2ApiConfig] = {
    val newInstance = RequiredOAuth2ApiConfig(IDs.next, behaviorVersion, api, maybeRecommendedScope, maybeApplication)
    (all += newInstance.toRaw).map(_ => newInstance)
  }

  def createFor(application: OAuth2Application, behaviorVersion: BehaviorVersion): DBIO[RequiredOAuth2ApiConfig] = {
    val raw = RawRequiredOAuth2ApiConfig(IDs.next, behaviorVersion.id, application.api.id, application.maybeScope, Some(application.id))
    (all += raw).map { _ =>
      RequiredOAuth2ApiConfig(
        raw.id,
        behaviorVersion,
        application.api,
        raw.maybeRecommendedScope,
        Some(application)
      )
    }
  }

  def maybeCreateFor(data: RequiredOAuth2ApiConfigData, behaviorVersion: BehaviorVersion): DBIO[Option[RequiredOAuth2ApiConfig]] = {
    for {
      maybeApi <- OAuth2ApiQueries.find(data.apiId)
      maybeApplication <- data.application.map { appData =>
        OAuth2ApplicationQueries.find(appData.applicationId)
      }.getOrElse(DBIO.successful(None))
      maybeConfig <- maybeApi.map { api =>
        createFor(
          behaviorVersion,
          api,
          data.recommendedScope,
          maybeApplication
        ).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield maybeConfig
  }
}
