package models

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import org.joda.time.{Seconds, DateTime}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global

case class OAuth2Token(
                        accessToken: String,
                        maybeSlackScopes: Option[String],
                        maybeTokenType: Option[String],
                        maybeExpirationTime: Option[DateTime],
                        maybeRefreshToken: Option[String],
                        loginInfo: LoginInfo
                        ) {
  def maybeOauth2Params: Option[Map[String, String]] = {
    maybeSlackScopes.map { scopes =>
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
}

class OAuth2TokensTable(tag: Tag) extends Table[OAuth2Token](tag, "oauth_2_tokens") {
  def accessToken = column[String]("access_token")
  def maybeSlackScopes = column[Option[String]]("slack_scopes")
  def maybeTokenType = column[Option[String]]("token_type")
  def maybeExpirationTime = column[Option[DateTime]]("expiration_time")
  def maybeRefreshToken = column[Option[String]]("refresh_token")
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")

  def loginInfo = (providerId, providerKey) <> (LoginInfo.tupled, LoginInfo.unapply _)

  def pk = primaryKey("oauth_2_tokens_pkey", (providerId, providerKey))

  def * = (accessToken, maybeSlackScopes, maybeTokenType, maybeExpirationTime, maybeRefreshToken, loginInfo) <> ((OAuth2Token.apply _).tupled, OAuth2Token.unapply _)
}

object OAuth2Token {
  val tokens = TableQuery[OAuth2TokensTable]

  def uncompiledFindByLoginInfo(providerId: Rep[String], providerKey: Rep[String]) = tokens.filter(_.providerId === providerId).filter(_.providerKey === providerKey)

  val findByLoginInfoQuery = Compiled(uncompiledFindByLoginInfo _)

  def save(loginInfo: LoginInfo, oauth2Info: OAuth2Info): DBIO[OAuth2Info] = {
    val expirationTime = oauth2Info.expiresIn.map { seconds => DateTime.now.plusSeconds(seconds) }
    val maybeSlackScopes = oauth2Info.params.flatMap { paramMap => paramMap.get("scope") }
    val token = OAuth2Token(oauth2Info.accessToken, maybeSlackScopes, oauth2Info.tokenType, expirationTime, oauth2Info.refreshToken, loginInfo)
    save(token).map { _ => oauth2Info }
  }

  def save(token: OAuth2Token): DBIO[OAuth2Token] = {
    val query = findByLoginInfoQuery(token.loginInfo.providerID, token.loginInfo.providerKey)
    query.result.headOption.flatMap { maybeToken =>
      maybeToken match {
        case Some(_) => query.update(token)
        case None => tokens += token
      }
    }.map { _ => token }
  }

  def deleteToken(token: String): DBIO[Int] = {
    tokens.filter(_.accessToken === token).delete
  }

  def deleteLoginInfo(loginInfo: LoginInfo): DBIO[Unit] = findByLoginInfoQuery(loginInfo.providerID, loginInfo.providerKey).delete.map { _ => () }

  def findByLoginInfo(loginInfo: LoginInfo): DBIO[Option[OAuth2Token]] = {
    findByLoginInfoQuery(loginInfo.providerID, loginInfo.providerKey).result.headOption
  }

  def uncompiledAllFullForSlackTeamIdQuery(teamId: Rep[String]) = {
    SlackProfileQueries.uncompiledAllForQuery(teamId).
      join(tokens).on(_.providerKey === _.providerKey).
      map { case(profile, token) => token }.
      filter(_.maybeSlackScopes.isDefined).
      filterNot(_.maybeSlackScopes === "identify")
  }
  val allFullForSlackTeamIdQuery = Compiled(uncompiledAllFullForSlackTeamIdQuery _)

  def allFullForSlackTeamId(teamId: String): DBIO[Seq[OAuth2Token]] = {
    allFullForSlackTeamIdQuery(teamId).result
  }

  def maybeFullForSlackTeamId(teamId: String): DBIO[Option[OAuth2Token]] = {
    allFullForSlackTeamId(teamId).map(_.headOption)
  }

  def maybeFullFor(loginInfo: LoginInfo): DBIO[Option[OAuth2Token]] = {
    SlackProfileQueries.find(loginInfo).flatMap { maybeProfile =>
      maybeProfile.map { profile =>
        maybeFullForSlackTeamId(profile.teamId)
      }.getOrElse {
        DBIO.successful(None)
      }
    }
  }
}
