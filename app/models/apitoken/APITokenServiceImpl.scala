package models.apitoken

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import models.IDs
import models.accounts.user.User
import services.DataService
import drivers.SlickPostgresDriver.api._
import models.behaviors.invocationtoken.InvocationToken

import scala.concurrent.{ExecutionContext, Future}

class APITokensTable(tag: Tag) extends Table[APIToken](tag, "api_tokens") {

  def id = column[String]("id", O.PrimaryKey)
  def label = column[String]("label")
  def userId = column[String]("user_id")
  def maybeExpirySeconds = column[Option[Int]]("expiry_seconds")
  def isOneTime = column[Boolean]("is_one_time")
  def isRevoked = column[Boolean]("is_revoked")
  def maybeLastUsed = column[Option[OffsetDateTime]]("last_used")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (id, label, userId, maybeExpirySeconds, isOneTime, isRevoked, maybeLastUsed, createdAt) <> ((APIToken.apply _).tupled, APIToken.unapply _)
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

  def create(newInstance: APIToken): Future[APIToken] = dataService.run((all += newInstance).map(_ => newInstance))

  def createFor(user: User, label: String): Future[APIToken] = {
    val newInstance = APIToken(IDs.next, label, user.id, maybeExpirySeconds = None, isOneTime = false, isRevoked = false, None, OffsetDateTime.now)
    create(newInstance)
  }

  def createFor(invocationToken: InvocationToken, maybeExpirySeconds: Option[Int], isOneTime: Boolean): Future[APIToken] = {
    val label = invocationToken.id // shrug
    val newInstance = APIToken(IDs.next, label, invocationToken.userId, maybeExpirySeconds, isOneTime, isRevoked = false, None, OffsetDateTime.now)
    create(newInstance)
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
