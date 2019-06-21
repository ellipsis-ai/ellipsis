package models.accounts.linkedoauth1token

import drivers.SlickPostgresDriver.api._
import javax.inject.{Inject, Provider}
import models.accounts.oauth1application.OAuth1ApplicationQueries
import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.User
import play.api.libs.ws.WSClient
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class RawLinkedOAuth1Token(
                                 accessToken: String,
                                 secret: String,
                                 userId: String,
                                 applicationId: String
                               )

class LinkedOAuth1TokensTable(tag: Tag) extends Table[RawLinkedOAuth1Token](tag, "linked_oauth1_tokens") {
  def accessToken = column[String]("access_token")
  def secret = column[String]("secret")
  def userId = column[String]("user_id")
  def applicationId = column[String]("application_id")

  def * = (accessToken, secret, userId, applicationId) <>
    ((RawLinkedOAuth1Token.apply _).tupled, RawLinkedOAuth1Token.unapply _)
}

class LinkedOAuth1TokenServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService],
                                               ws: WSClient,
                                               implicit val ec: ExecutionContext
                                             ) extends LinkedOAuth1TokenService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[LinkedOAuth1TokensTable]
  val allWithApplication = all.join(OAuth1ApplicationQueries.allWithApi).on(_.applicationId === _._1.id)

  type TupleType = (RawLinkedOAuth1Token, OAuth1ApplicationQueries.TupleType)

  def tuple2Token(tuple: TupleType): LinkedOAuth1Token = {
    val raw = tuple._1
    LinkedOAuth1Token(
      raw.accessToken,
      raw.secret,
      raw.userId,
      OAuth1ApplicationQueries.tuple2Application(tuple._2)
    )
  }

  def uncompiledAllSharedForTeamIdQuery(teamId: Rep[String]) = {
    allWithApplication.
      filter(_._2._1.maybeSharedTokenUserId.isDefined).
      filter { case(_, (app, _)) => app.teamId === teamId || app.isShared }
  }
  val allSharedForTeamIdQuery = Compiled(uncompiledAllSharedForTeamIdQuery _)

  def sharedForUserAction(user: User, ws: WSClient): DBIO[Seq[LinkedOAuth1Token]] = {
    allSharedForTeamIdQuery(user.teamId).result.map { r =>
      r.map(tuple2Token)
    }
  }

  def uncompiledAllForUserIdQuery(userId: Rep[String]) = {
    allWithApplication.filter(_._1.userId === userId)
  }
  val allForUserIdQuery = Compiled(uncompiledAllForUserIdQuery _)

  def allForUserAction(user: User, ws: WSClient): DBIO[Seq[LinkedOAuth1Token]] = {
    allForUserIdQuery(user.id).result.map { r =>
      r.map(tuple2Token)
    }
  }

  def allForUser(user: User, ws: WSClient): Future[Seq[LinkedOAuth1Token]] = {
    dataService.run(allForUserAction(user, ws))
  }

  def uncompiledFindQuery(userId: Rep[String], applicationId: Rep[String]) = {
    all.filter(_.userId === userId).filter(_.applicationId === applicationId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def saveAction(token: LinkedOAuth1Token): DBIO[LinkedOAuth1Token] = {
    val query = findQuery(token.userId, token.application.id)
    val raw = token.toRaw
    query.result.headOption.flatMap {
      case Some(_) => query.update(raw)
      case None => all += raw
    }.map { _ => token }
  }

  def save(token: LinkedOAuth1Token): Future[LinkedOAuth1Token] = {
    dataService.run(saveAction(token))
  }

  def deleteFor(application: OAuth2Application, user: User): Future[Boolean] = {
    val action = findQuery(user.id, application.id).delete.map(r => r > 0)
    dataService.run(action)
  }
}
