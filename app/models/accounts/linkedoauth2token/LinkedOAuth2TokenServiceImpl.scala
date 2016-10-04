package models.accounts.linkedoauth2token

import javax.inject.{Inject, Provider}

import com.github.tototoshi.slick.PostgresJodaSupport._
import models.accounts.user.User
import models.accounts.oauth2application.OAuth2ApplicationQueries
import org.joda.time.DateTime
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.ws.WSClient
import play.api.mvc.Results
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawLinkedOAuth2Token(
                                 accessToken: String,
                                 maybeTokenType: Option[String],
                                 maybeExpirationTime: Option[DateTime],
                                 maybeRefreshToken: Option[String],
                                 maybeScopeGranted: Option[String],
                                 userId: String,
                                 applicationId: String
                               )

class LinkedOAuth2TokensTable(tag: Tag) extends Table[RawLinkedOAuth2Token](tag, "linked_oauth2_tokens") {
  def accessToken = column[String]("access_token")
  def maybeTokenType = column[Option[String]]("token_type")
  def maybeExpirationTime = column[Option[DateTime]]("expiration_time")
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

  def allForUser(user: User, ws: WSClient): Future[Seq[LinkedOAuth2Token]] = {
    dataService.run(allForUserIdQuery(user.id).result).flatMap { r =>
      Future.sequence(r.map(tuple2Token).map(refreshIfNecessary))
    }
  }

  private def refreshIfNecessary(linkedOAuth2Token: LinkedOAuth2Token): Future[LinkedOAuth2Token] = {
    val eventualMaybeNewInstance = if (linkedOAuth2Token.isExpired) {
      linkedOAuth2Token.maybeRefreshToken.map { token =>
        val tokenResponse = linkedOAuth2Token.application.refreshTokenRequestFor(token, ws).
          withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
          post(Results.EmptyContent())

        tokenResponse.flatMap { response =>
          val json = response.json
          (json \ "access_token").asOpt[String].map { accessToken =>
            val maybeTokenType = (json \ "token_type").asOpt[String]
            val maybeScopeGranted = (json \ "scope").asOpt[String]
            val maybeExpirationTime = (json \ "expires_in").asOpt[Int].map { seconds =>
              DateTime.now.plusSeconds(seconds)
            }
            save(linkedOAuth2Token.copy(
              accessToken = accessToken,
              maybeScopeGranted = maybeScopeGranted,
              maybeExpirationTime = maybeExpirationTime,
              maybeTokenType = maybeTokenType
            )).map(Some(_))
          }.getOrElse(Future.successful(None))
        }
      }.getOrElse(Future.successful(None))
    } else {
      Future.successful(None)
    }

    eventualMaybeNewInstance.map { maybeNewInstance =>
      maybeNewInstance.getOrElse(linkedOAuth2Token)
    }
  }

  def uncompiledFindQuery(userId: Rep[String], applicationId: Rep[String]) = {
    all.filter(_.userId === userId).filter(_.applicationId === applicationId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def save(token: LinkedOAuth2Token): Future[LinkedOAuth2Token] = {
    val query = findQuery(token.userId, token.application.id)
    val raw = token.toRaw
    val action = query.result.headOption.flatMap {
      case Some(_) => query.update(raw)
      case None => all += raw
    }.map { _ => token }
    dataService.run(action)
  }


}
