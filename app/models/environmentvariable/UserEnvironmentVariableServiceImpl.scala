package models.environmentvariable

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import models.accounts.user.{User, UserQueries}
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawUserEnvironmentVariable(
                                   name: String,
                                   value: String,
                                   userId: String,
                                   createdAt: OffsetDateTime
                                 )

class UserEnvironmentVariablesTable(tag: Tag) extends Table[RawUserEnvironmentVariable](tag, "user_environment_variables") {

  def name = column[String]("name")
  def value = column[String]("value")
  def userId = column[String]("user_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (name, value, userId, createdAt) <> ((RawUserEnvironmentVariable.apply _).tupled, RawUserEnvironmentVariable.unapply _)
}

class UserEnvironmentVariableServiceImpl @Inject() (
                                                 dataServiceProvider: Provider[DataService]
                                               ) extends UserEnvironmentVariableService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[UserEnvironmentVariablesTable]
  val allWithUser = all.join(UserQueries.all).on(_.userId === _.id)

  def tuple2EnvironmentVariable(tuple: (RawUserEnvironmentVariable, User)): UserEnvironmentVariable = {
    val raw = tuple._1
    UserEnvironmentVariable(raw.name, raw.value, tuple._2, raw.createdAt)
  }

  def uncompiledFindQueryFor(name: Rep[String], userId: Rep[String]) = {
    allWithUser.
      filter { case(envVar, user) => envVar.name === name }.
      filter { case(envVar, user) => user.id === userId }
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def uncompiledRawFindQueryFor(name: Rep[String], userId: Rep[String]) = {
    all.filter(_.name === name).filter(_.userId === userId)
  }
  val rawFindQueryFor = Compiled(uncompiledRawFindQueryFor _)

  def find(name: String, user: User): Future[Option[UserEnvironmentVariable]] = {
    dataService.run(findQueryFor(name, user.id).result.map { r => r.headOption.map(tuple2EnvironmentVariable) })
  }

  def ensureFor(name: String, maybeValue: Option[String], user: User): Future[Option[UserEnvironmentVariable]] = {
    val action = Option(name).filter(_.trim.nonEmpty).map { nonEmptyName =>
      val query = rawFindQueryFor(name, user.id)
      query.result.flatMap { r =>
        r.headOption.map { existing =>
          maybeValue.map { value =>
            val raw = RawUserEnvironmentVariable(name, value, user.id, OffsetDateTime.now)
            query.update(raw).map(_ => raw)
          }.getOrElse(DBIO.successful(existing))
        }.getOrElse {
          val value = maybeValue.getOrElse("")
          val raw = RawUserEnvironmentVariable(name, value, user.id, OffsetDateTime.now)
          (all += raw).map(_ => raw)
        }.map { raw =>
          Some(UserEnvironmentVariable(raw.name, raw.value, user, raw.createdAt))
        }
      }
    }.getOrElse(DBIO.successful(None))
    dataService.run(action)
  }

  def deleteFor(name: String, user: User): Future[Boolean] = {
    val action = rawFindQueryFor(name, user.id).delete.map( result => result > 0)
    dataService.run(action)
  }

  def uncompiledAllForUserQuery(userId: Rep[String]) = allWithUser.filter(_._1.userId === userId)
  val allForUserQuery = Compiled(uncompiledAllForUserQuery _)

  def allForAction(user: User): DBIO[Seq[UserEnvironmentVariable]] = {
    allForUserQuery(user.id).result.map { r => r.map(tuple2EnvironmentVariable)}
  }

  def allFor(user: User): Future[Seq[UserEnvironmentVariable]] = {
    dataService.run(allForAction(user))
  }
}
