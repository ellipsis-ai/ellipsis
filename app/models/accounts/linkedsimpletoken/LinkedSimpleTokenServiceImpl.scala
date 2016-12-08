package models.accounts.linkedsimpletoken

import javax.inject.{Inject, Provider}

import models.accounts.user.User
import models.accounts.simpletokenapi.{SimpleTokenApi, SimpleTokenApiQueries}
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawLinkedSimpleToken(
                                 accessToken: String,
                                 userId: String,
                                 apiId: String
                               )

class LinkedSimpleTokensTable(tag: Tag) extends Table[RawLinkedSimpleToken](tag, "linked_simple_tokens") {
  def accessToken = column[String]("access_token")
  def userId = column[String]("user_id")
  def apiId = column[String]("api_id")

  def * = (accessToken, userId, apiId) <>
    ((RawLinkedSimpleToken.apply _).tupled, RawLinkedSimpleToken.unapply _)
}

class LinkedSimpleTokenServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService]
                                             ) extends LinkedSimpleTokenService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[LinkedSimpleTokensTable]
  val allWithApi = all.join(SimpleTokenApiQueries.all).on(_.apiId === _.id)

  type TupleType = (RawLinkedSimpleToken, SimpleTokenApi)

  def tuple2Token(tuple: TupleType): LinkedSimpleToken = {
    val raw = tuple._1
    LinkedSimpleToken(
      raw.accessToken,
      raw.userId,
      tuple._2
    )
  }

  def uncompiledAllForUserIdQuery(userId: Rep[String]) = {
    allWithApi.filter(_._1.userId === userId)
  }
  val allForUserIdQuery = Compiled(uncompiledAllForUserIdQuery _)

  def allForUser(user: User): Future[Seq[LinkedSimpleToken]] = {
    dataService.run(allForUserIdQuery(user.id).result).map { r =>
      r.map(tuple2Token)
    }
  }

  def uncompiledFindQuery(userId: Rep[String], apiId: Rep[String]) = {
    all.filter(_.userId === userId).filter(_.apiId === apiId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def save(token: LinkedSimpleToken): Future[LinkedSimpleToken] = {
    val query = findQuery(token.userId, token.api.id)
    val raw = token.toRaw
    val action = query.result.headOption.flatMap {
      case Some(_) => query.update(raw)
      case None => all += raw
    }.map { _ => token }
    dataService.run(action)
  }


}
