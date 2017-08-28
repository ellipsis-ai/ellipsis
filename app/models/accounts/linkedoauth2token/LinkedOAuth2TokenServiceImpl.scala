package models.accounts.linkedoauth2token

import java.time.OffsetDateTime
import javax.inject.{Inject, Provider}

import models.accounts.user.User
import models.accounts.oauth2application.{OAuth2Application, OAuth2ApplicationQueries}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.ws.WSClient
import play.api.mvc.Results
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawLinkedOAuth2Token(
                                 accessToken: String,
                                 maybeTokenType: Option[String],
                                 maybeExpirationTime: Option[OffsetDateTime],
                                 maybeRefreshToken: Option[String],
                                 maybeScopeGranted: Option[String],
                                 userId: String,
                                 applicationId: String
                               )

class LinkedOAuth2TokensTable(tag: Tag) extends Table[RawLinkedOAuth2Token](tag, "linked_oauth2_tokens") {
  def accessToken = column[String]("access_token")
  def maybeTokenType = column[Option[String]]("token_type")
  def maybeExpirationTime = column[Option[OffsetDateTime]]("expiration_time")
  def maybeRefreshToken = column[Option[String]]("refresh_token")
  def maybeScopeGranted = column[Option[String]]("scope_granted")
  def userId = column[String]("user_id")
  def applicationId = column[String]("config_id")

  def * = (accessToken, maybeTokenType, maybeExpirationTime, maybeRefreshToken, maybeScopeGranted, userId, applicationId) <>
    ((RawLinkedOAuth2Token.apply _).tupled, RawLinkedOAuth2Token.unapply _)
}

class LinkedOAuth2TokenServiceImpl @Inject() (
                                               dataServiceProvider: Provider[DataService],
                                               ws: WSClient
                                           ) extends LinkedOAuth2TokenService {

  def dataService = dataServiceProvider.get

  val all = TableQuery[LinkedOAuth2TokensTable]
  val allWithApplication = all.join(OAuth2ApplicationQueries.allWithApi).on(_.applicationId === _._1.id)

  type TupleType = (RawLinkedOAuth2Token, OAuth2ApplicationQueries.TupleType)

  def tuple2Token(tuple: TupleType): LinkedOAuth2Token = {
    val raw = tuple._1
    LinkedOAuth2Token(
      raw.accessToken,
      raw.maybeTokenType,
      raw.maybeExpirationTime,
      raw.maybeRefreshToken,
      raw.maybeScopeGranted,
      raw.userId,
      OAuth2ApplicationQueries.tuple2Application(tuple._2)
    )
  }

  def uncompiledAllForUserIdQuery(userId: Rep[String]) = {
    allWithApplication.filter(_._1.userId === userId)
  }
  val allForUserIdQuery = Compiled(uncompiledAllForUserIdQuery _)

  def allForUserAction(user: User, ws: WSClient): DBIO[Seq[LinkedOAuth2Token]] = {
    allForUserIdQuery(user.id).result.flatMap { r =>
      DBIO.sequence(r.map(tuple2Token).map(refreshIfNecessaryAction))
    }
  }

  def allForUser(user: User, ws: WSClient): Future[Seq[LinkedOAuth2Token]] = {
    dataService.run(allForUserAction(user, ws))
  }

  private def refreshIfNecessaryAction(linkedOAuth2Token: LinkedOAuth2Token): DBIO[LinkedOAuth2Token] = {
    val eventualMaybeNewInstance = linkedOAuth2Token.maybeRefreshToken.map { token =>
      if (linkedOAuth2Token.maybeExpirationTime.isEmpty || linkedOAuth2Token.isExpiredOrExpiresSoon) {
        val tokenResponse = linkedOAuth2Token.application.refreshTokenResponseFor(token, ws)

        DBIO.from(tokenResponse).flatMap { response =>
          val json = response.json
          (json \ "access_token").asOpt[String].map { accessToken =>
            val maybeTokenType = (json \ "token_type").asOpt[String]
            val maybeScopeGranted = (json \ "scope").asOpt[String]
            val maybeExpirationTime = (json \ "expires_in").asOpt[Int].map { seconds =>
              OffsetDateTime.now.plusSeconds(seconds)
            }
            saveAction(linkedOAuth2Token.copy(
              accessToken = accessToken,
              maybeScopeGranted = maybeScopeGranted,
              maybeExpirationTime = maybeExpirationTime,
              maybeTokenType = maybeTokenType
            )).map(Some(_))
          }.getOrElse(DBIO.successful(None))
        }
      } else {
        DBIO.successful(None)
      }
    }.getOrElse(DBIO.successful(None))

    eventualMaybeNewInstance.map { maybeNewInstance =>
      maybeNewInstance.getOrElse(linkedOAuth2Token)
    }
  }

  def uncompiledFindQuery(userId: Rep[String], applicationId: Rep[String]) = {
    all.filter(_.userId === userId).filter(_.applicationId === applicationId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def saveAction(token: LinkedOAuth2Token): DBIO[LinkedOAuth2Token] = {
    val query = findQuery(token.userId, token.application.id)
    val raw = token.toRaw
    query.result.headOption.flatMap {
      case Some(_) => query.update(raw)
      case None => all += raw
    }.map { _ => token }
  }

  def save(token: LinkedOAuth2Token): Future[LinkedOAuth2Token] = {
    dataService.run(saveAction(token))
  }

  def deleteFor(application: OAuth2Application, user: User): Future[Boolean] = {
    val action = findQuery(user.id, application.id).delete.map(r => r > 0)
    dataService.run(action)
  }
}
