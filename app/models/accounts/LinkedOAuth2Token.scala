package models.accounts

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import org.joda.time.{DateTime, Seconds}
import play.api.http.{MimeTypes, HeaderNames}
import play.api.libs.ws.WSClient
import play.api.mvc.Results
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class LinkedOAuth2Token(
                              accessToken: String,
                              maybeTokenType: Option[String],
                              maybeExpirationTime: Option[DateTime],
                              maybeRefreshToken: Option[String],
                              maybeScopeGranted: Option[String],
                              userId: String,
                              config: CustomOAuth2Configuration
                              ) {

  val maybeScope: Option[String] = config.maybeScope

  def maybeOauth2Params: Option[Map[String, String]] = {
    maybeScope.map { scopes =>
      Map("scope" -> scopes)
    }
  }
  def oauth2Info: OAuth2Info = OAuth2Info(accessToken, maybeTokenType, expiresIn, maybeRefreshToken, maybeOauth2Params)
  def expiresIn: Option[Int] = maybeExpirationTime.map { expirationTime =>
    val now = DateTime.now
    if (expirationTime.isAfter(now)) {
      Seconds.secondsBetween(now, expirationTime).getSeconds
    } else {
      0
    }
  }

  def isExpired: Boolean = maybeExpirationTime.exists(_.isBeforeNow)

  def refreshIfNecessary(ws: WSClient): DBIO[LinkedOAuth2Token] = {
    val eventualMaybeNewInstance = if (isExpired) {
      maybeRefreshToken.map { token =>
        val tokenResponse = config.refreshTokenRequestFor(token, ws).
          withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
          post(Results.EmptyContent())

        DBIO.from(tokenResponse).flatMap { response =>
          val json = response.json
          (json \ "access_token").asOpt[String].map { accessToken =>
            val maybeTokenType = (json \ "token_type").asOpt[String]
            val maybeScopeGranted = (json \ "scope").asOpt[String]
            val maybeExpirationTime = (json \ "expires_in").asOpt[Int].map { seconds =>
              DateTime.now.plusSeconds(seconds)
            }
            copy(
              accessToken = accessToken,
              maybeScopeGranted = maybeScopeGranted,
              maybeExpirationTime = maybeExpirationTime,
              maybeTokenType = maybeTokenType
            ).save.map(Some(_))
          }.getOrElse(DBIO.successful(None))
        }
      }.getOrElse(DBIO.successful(None))
    } else {
      DBIO.successful(None)
    }

    eventualMaybeNewInstance.map { maybeNewInstance =>
      maybeNewInstance.getOrElse(this)
    }
  }

  def save: DBIO[LinkedOAuth2Token] = LinkedOAuth2TokenQueries.save(this)

  def toRaw: RawLinkedOAuth2Token = RawLinkedOAuth2Token(
    accessToken,
    maybeTokenType,
    maybeExpirationTime,
    maybeRefreshToken,
    maybeScopeGranted,
    userId,
    config.id
  )

}

case class RawLinkedOAuth2Token(
                                 accessToken: String,
                                 maybeTokenType: Option[String],
                                 maybeExpirationTime: Option[DateTime],
                                 maybeRefreshToken: Option[String],
                                 maybeScopeGranted: Option[String],
                                 userId: String,
                                 configId: String
                                 )

class LinkedOAuth2TokensTable(tag: Tag) extends Table[RawLinkedOAuth2Token](tag, "linked_oauth_2_tokens") {
  def accessToken = column[String]("access_token")
  def maybeTokenType = column[Option[String]]("token_type")
  def maybeExpirationTime = column[Option[DateTime]]("expiration_time")
  def maybeRefreshToken = column[Option[String]]("refresh_token")
  def maybeScopeGranted = column[Option[String]]("scope_granted")
  def userId = column[String]("user_id")
  def configId = column[String]("config_id")

  def * = (accessToken, maybeTokenType, maybeExpirationTime, maybeRefreshToken, maybeScopeGranted, userId, configId) <>
    ((RawLinkedOAuth2Token.apply _).tupled, RawLinkedOAuth2Token.unapply _)
}

object LinkedOAuth2TokenQueries {

  val all = TableQuery[LinkedOAuth2TokensTable]
  val allWithConfig = all.join(CustomOAuth2ConfigurationQueries.allWithTemplate).on(_.configId === _._1.id)

  type TupleType = (RawLinkedOAuth2Token, CustomOAuth2ConfigurationQueries.TupleType)

  def tuple2Token(tuple: TupleType): LinkedOAuth2Token = {
    val raw = tuple._1
    LinkedOAuth2Token(
      raw.accessToken,
      raw.maybeTokenType,
      raw.maybeExpirationTime,
      raw.maybeRefreshToken,
      raw.maybeScopeGranted,
      raw.userId,
      CustomOAuth2ConfigurationQueries.tuple2Config(tuple._2)
    )
  }

  def uncompiledAllForUserIdQuery(userId: Rep[String]) = {
    allWithConfig.filter(_._1.userId === userId)
  }
  val allForUserIdQuery = Compiled(uncompiledAllForUserIdQuery _)

  def allForUser(user: User, ws: WSClient): DBIO[Seq[LinkedOAuth2Token]] = {
    allForUserIdQuery(user.id).result.flatMap { r =>
      DBIO.sequence(r.map(tuple2Token).map(_.refreshIfNecessary(ws)))
    }
  }

  def uncompiledFindQuery(userId: Rep[String], configId: Rep[String]) = {
    all.filter(_.userId === userId).filter(_.configId === configId)
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def save(token: LinkedOAuth2Token): DBIO[LinkedOAuth2Token] = {
    val query = findQuery(token.userId, token.config.id)
    val raw = token.toRaw
    query.result.headOption.flatMap { maybeToken =>
      maybeToken match {
        case Some(_) => query.update(raw)
        case None => all += raw
      }
    }.map { _ => token }
  }

}
