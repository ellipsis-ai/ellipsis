package models.apitoken

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.accounts.user.User
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

class APITokensTable(tag: Tag) extends Table[APIToken](tag, "api_tokens") {

  def id = column[String]("id", O.PrimaryKey)
  def label = column[String]("label")
  def userId = column[String]("user_id")
  def isRevoked = column[Boolean]("is_revoked")
  def maybeLastUsed = column[Option[OffsetDateTime]]("last_used")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, label, userId, isRevoked, maybeLastUsed, createdAt) <> ((APIToken.apply _).tupled, APIToken.unapply _)
}

class APITokenServiceImpl @Inject() (
                                  dataServiceProvider: Provider[DataService],
                                  implicit val ec: ExecutionContext
                                ) extends APITokenService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[APITokensTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(id: String): Future[Option[APIToken]] = {
    dataService.run(findQueryFor(id).result.map(_.headOption))
  }

  def createFor(user: User, label: String): Future[APIToken] = {
    val newInstance = APIToken(IDs.next, label, user.id, isRevoked = false, None, OffsetDateTime.now)
    dataService.run((all += newInstance).map(_ => newInstance))
  }

  def uncompiledAllForQuery(userId: Rep[String]) = {
    all.filter(_.userId === userId).filterNot(_.isRevoked)
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(user: User): Future[Seq[APIToken]] = {
    dataService.run(allForQuery(user.id).result)
  }

  def use(token: APIToken): Future[APIToken] = {
    val updated = token.copy(maybeLastUsed = Some(OffsetDateTime.now))
    dataService.run(findQueryFor(token.id).update(updated).map(_ => updated))
  }

  def revoke(token: APIToken): Future[APIToken] = {
    val updated = token.copy(isRevoked = true)
    dataService.run(findQueryFor(token.id).update(updated).map(_ => updated))
  }
}
