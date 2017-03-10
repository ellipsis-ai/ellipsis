package models.behaviors.config.requiredsimpletokenapi

import javax.inject.Inject

import com.google.inject.Provider
import json.RequiredSimpleTokenApiData
import models.IDs
import models.accounts.simpletokenapi.{SimpleTokenApi, SimpleTokenApiQueries}
import models.accounts.user.User
import models.behaviors.behaviorversion.{BehaviorVersion, BehaviorVersionQueries}
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawRequiredSimpleTokenApi(
                                           id: String,
                                           behaviorVersionId: String,
                                           apiId: String
                                         )

class RequiredSimpleTokenApisTable(tag: Tag) extends Table[RawRequiredSimpleTokenApi](tag, "required_simple_token_apis") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorVersionId = column[String]("behavior_version_id")
  def apiId = column[String]("api_id")

  def * = (id, behaviorVersionId, apiId) <>
    ((RawRequiredSimpleTokenApi.apply _).tupled, RawRequiredSimpleTokenApi.unapply _)
}

class RequiredSimpleTokenApiServiceImpl @Inject()(
                                                         dataServiceProvider: Provider[DataService]
                                                       ) extends RequiredSimpleTokenApiService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[RequiredSimpleTokenApisTable]
  val allWithBehaviorVersion = all.join(BehaviorVersionQueries.allWithGroupVersion).on(_.behaviorVersionId === _._1._1._1.id)
  val allWithApi = allWithBehaviorVersion.join(SimpleTokenApiQueries.all).on(_._1.apiId === _.id)

  type TupleType = ((RawRequiredSimpleTokenApi, BehaviorVersionQueries.TupleType), SimpleTokenApi)

  def tuple2Required(tuple: TupleType): RequiredSimpleTokenApi = {
    val raw = tuple._1._1
    val behaviorVersion = BehaviorVersionQueries.tuple2BehaviorVersion(tuple._1._2)
    RequiredSimpleTokenApi(
      raw.id,
      behaviorVersion,
      tuple._2
    )
  }

  def uncompiledAllForQuery(behaviorVersionId: Rep[String]) = {
    allWithApi.filter { case((required, _), _) => required.behaviorVersionId === behaviorVersionId }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(behaviorVersion: BehaviorVersion): Future[Seq[RequiredSimpleTokenApi]] = {
    val action = allForQuery(behaviorVersion.id).result.map(r => r.map(tuple2Required))
    dataService.run(action)
  }

  def uncompiledAllForApiAndVersionQuery(apiId: Rep[String], behaviorVersionId: Rep[String]) = {
    allWithApi.
      filter { case((required, _), _) => required.apiId === apiId }.
      filter { case((required, _), _) => required.behaviorVersionId === behaviorVersionId }
  }
  val allForApiAndVersionQuery = Compiled(uncompiledAllForApiAndVersionQuery _)

  def allFor(api: SimpleTokenApi, behaviorVersion: BehaviorVersion): Future[Seq[RequiredSimpleTokenApi]] = {
    val action = allForApiAndVersionQuery(api.id, behaviorVersion.id).result.map { r =>
      r.map(tuple2Required)
    }
    dataService.run(action)
  }

  def missingFor(user: User, behaviorVersion: BehaviorVersion): Future[Seq[RequiredSimpleTokenApi]] = {
    for {
      required <- allFor(behaviorVersion)
      linked <- dataService.linkedSimpleTokens.allForUser(user)
    } yield {
      required.filterNot { r =>
        linked.exists { link => link.api == r.api }
      }
    }
  }

  def uncompiledFindQuery(id: Rep[String]) = allWithApi.filter { case((required, _), _) => required.id === id }
  val findQuery = Compiled(uncompiledFindQuery _)

  def find(id: String): Future[Option[RequiredSimpleTokenApi]] = {
    val action = findQuery(id).result.map { r =>
      r.headOption.map(tuple2Required)
    }
    dataService.run(action)
  }

  def uncompiledFindRawQuery(id: Rep[String]) = all.filter(_.id === id)
  val findRawQuery = Compiled(uncompiledFindRawQuery _)

  def save(requiredApiConfig: RequiredSimpleTokenApi): Future[RequiredSimpleTokenApi] = {
    val query = findRawQuery(requiredApiConfig.id)
    val raw = requiredApiConfig.toRaw
    val action = query.result.flatMap { r =>
      r.headOption.map { _ =>
        query.update(raw)
      }.getOrElse {
        all += raw
      }
    }.map(_ => requiredApiConfig)
    dataService.run(action)
  }

  def maybeCreateFor(data: RequiredSimpleTokenApiData, behaviorVersion: BehaviorVersion): Future[Option[RequiredSimpleTokenApi]] = {
    val action = for {
      maybeApi <- DBIO.from(dataService.simpleTokenApis.find(data.apiId))
      maybeConfig <- maybeApi.map { api =>
        val newInstance = RequiredSimpleTokenApi(IDs.next, behaviorVersion, api)
        (all += newInstance.toRaw).map(_ => newInstance).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield maybeConfig

    dataService.run(action)
  }
}
