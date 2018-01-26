package models.behaviors.config.requiredsimpletokenapi

import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.RequiredSimpleTokenApiData
import models.IDs
import models.accounts.simpletokenapi.{SimpleTokenApi, SimpleTokenApiQueries}
import models.accounts.user.User
import models.behaviors.behaviorgroupversion.{BehaviorGroupVersion, BehaviorGroupVersionQueries}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class RawRequiredSimpleTokenApi(
                                      id: String,
                                      exportId: String,
                                      groupVersionId: String,
                                      nameInCode: String,
                                      apiId: String
                                         )

class RequiredSimpleTokenApisTable(tag: Tag) extends Table[RawRequiredSimpleTokenApi](tag, "required_simple_token_apis") {

  def id = column[String]("id", O.PrimaryKey)
  def exportId = column[String]("export_id")
  def groupVersionId = column[String]("group_version_id")
  def nameInCode = column[String]("name_in_code")
  def apiId = column[String]("api_id")

  def * = (id, exportId, groupVersionId, nameInCode, apiId) <>
    ((RawRequiredSimpleTokenApi.apply _).tupled, RawRequiredSimpleTokenApi.unapply _)
}

class RequiredSimpleTokenApiServiceImpl @Inject()(
                                                         dataServiceProvider: Provider[DataService],
                                                         implicit val ec: ExecutionContext
                                                       ) extends RequiredSimpleTokenApiService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[RequiredSimpleTokenApisTable]
  val allWithGroupVersion = all.join(BehaviorGroupVersionQueries.allWithUser).on(_.groupVersionId === _._1._1.id)
  val allWithApi = allWithGroupVersion.join(SimpleTokenApiQueries.all).on(_._1.apiId === _.id)

  type TupleType = ((RawRequiredSimpleTokenApi, BehaviorGroupVersionQueries.TupleType), SimpleTokenApi)

  def tuple2Required(tuple: TupleType): RequiredSimpleTokenApi = {
    val raw = tuple._1._1
    val groupVersion = BehaviorGroupVersionQueries.tuple2BehaviorGroupVersion(tuple._1._2)
    RequiredSimpleTokenApi(
      raw.id,
      raw.exportId,
      groupVersion,
      raw.nameInCode,
      tuple._2
    )
  }

  def uncompiledAllForQuery(behaviorVersionId: Rep[String]) = {
    allWithApi.
      filter { case((required, _), _) => required.groupVersionId === behaviorVersionId }.
      sortBy { case((required, _), _) => required.nameInCode }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allForIdAction(groupVersionId: String): DBIO[Seq[RequiredSimpleTokenApi]] = {
    allForQuery(groupVersionId).result.map(r => r.map(tuple2Required))
  }

  def allForAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[RequiredSimpleTokenApi]] = {
    allForIdAction(groupVersion.id)
  }

  def allForId(groupVersionId: String): Future[Seq[RequiredSimpleTokenApi]] = {
    dataService.run(allForIdAction(groupVersionId))
  }

  def allFor(groupVersion: BehaviorGroupVersion): Future[Seq[RequiredSimpleTokenApi]] = {
    dataService.run(allForAction(groupVersion))
  }

  def uncompiledAllForApiAndVersionQuery(apiId: Rep[String], behaviorVersionId: Rep[String]) = {
    allWithApi.
      filter { case((required, _), _) => required.apiId === apiId }.
      filter { case((required, _), _) => required.groupVersionId === behaviorVersionId }
  }
  val allForApiAndVersionQuery = Compiled(uncompiledAllForApiAndVersionQuery _)

  def allFor(api: SimpleTokenApi, groupVersion: BehaviorGroupVersion): Future[Seq[RequiredSimpleTokenApi]] = {
    val action = allForApiAndVersionQuery(api.id, groupVersion.id).result.map { r =>
      r.map(tuple2Required)
    }
    dataService.run(action)
  }

  def missingFor(user: User, groupVersion: BehaviorGroupVersion): Future[Seq[RequiredSimpleTokenApi]] = {
    for {
      required <- allFor(groupVersion)
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

  def maybeCreateForAction(data: RequiredSimpleTokenApiData, groupVersion: BehaviorGroupVersion): DBIO[Option[RequiredSimpleTokenApi]] = {
    for {
      maybeApi <- DBIO.from(dataService.simpleTokenApis.find(data.apiId))
      maybeConfig <- maybeApi.map { api =>
        val newInstance = RequiredSimpleTokenApi(IDs.next, data.exportId.getOrElse(IDs.next), groupVersion, data.nameInCode, api)
        (all += newInstance.toRaw).map(_ => newInstance).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield maybeConfig
  }

}
