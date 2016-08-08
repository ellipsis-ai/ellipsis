package models.accounts

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import org.joda.time.{DateTime, Seconds}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class LinkedOAuth2Token(
                              accessToken: String,
                              maybeTokenType: Option[String],
                              maybeExpirationTime: Option[DateTime],
                              maybeRefreshToken: Option[String],
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

  def save: DBIO[LinkedOAuth2Token] = LinkedOAuth2TokenQueries.save(this)

  def toRaw: RawLinkedOAuth2Token = RawLinkedOAuth2Token(
    accessToken,
    maybeTokenType,
    maybeExpirationTime,
    maybeRefreshToken,
    userId,
    config.id
  )

}

object LinkedOAuth2Token {

  def apply(info: OAuth2Info, user: User, config: CustomOAuth2Configuration): LinkedOAuth2Token = {
    val maybeExpirationTime = info.expiresIn.map { seconds =>
      DateTime.now.plusSeconds(seconds)
    }
    LinkedOAuth2Token(info.accessToken, info.tokenType, maybeExpirationTime, info.refreshToken, user.id, config)
  }
}

case class RawLinkedOAuth2Token(
                                 accessToken: String,
                                 maybeTokenType: Option[String],
                                 maybeExpirationTime: Option[DateTime],
                                 maybeRefreshToken: Option[String],
                                 userId: String,
                                 configId: String
                                 )

class LinkedOAuth2TokensTable(tag: Tag) extends Table[RawLinkedOAuth2Token](tag, "linked_oauth_2_tokens") {
  def accessToken = column[String]("access_token")
  def maybeTokenType = column[Option[String]]("token_type")
  def maybeExpirationTime = column[Option[DateTime]]("expiration_time")
  def maybeRefreshToken = column[Option[String]]("refresh_token")
  def userId = column[String]("user_id")
  def configId = column[String]("config_id")

  def * = (accessToken, maybeTokenType, maybeExpirationTime, maybeRefreshToken, userId, configId) <>
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
      raw.userId,
      CustomOAuth2ConfigurationQueries.tuple2Config(tuple._2)
    )
  }

  def uncompiledAllForUserIdQuery(userId: Rep[String]) = {
    allWithConfig.filter(_._1.userId === userId)
  }
  val allForUserIdQuery = Compiled(uncompiledAllForUserIdQuery _)

  def allForUser(user: User): DBIO[Seq[LinkedOAuth2Token]] = {
    allForUserIdQuery(user.id).result.map { r =>
      r.map(tuple2Token)
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
