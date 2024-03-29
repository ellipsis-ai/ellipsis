package models.accounts.oauth2token

import java.time.OffsetDateTime
import javax.inject.{Inject, Provider}

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

class OAuth2TokensTable(tag: Tag) extends Table[OAuth2Token](tag, "oauth_2_tokens") {
  def accessToken = column[String]("access_token")
  def maybeSlackScopes = column[Option[String]]("slack_scopes")
  def maybeTokenType = column[Option[String]]("token_type")
  def maybeExpirationTime = column[Option[OffsetDateTime]]("expiration_time")
  def maybeRefreshToken = column[Option[String]]("refresh_token")
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")

  def loginInfo = (providerId, providerKey) <> (LoginInfo.tupled, LoginInfo.unapply _)

  def pk = primaryKey("oauth_2_tokens_pkey", (providerId, providerKey))

  def * = (accessToken, maybeSlackScopes, maybeTokenType, maybeExpirationTime, maybeRefreshToken, loginInfo) <> ((OAuth2Token.apply _).tupled, OAuth2Token.unapply _)
}

class OAuth2TokenServiceImpl @Inject() (
                                         dataServiceProvider: Provider[DataService],
                                         implicit val ec: ExecutionContext
                                       ) extends OAuth2TokenService {

  def dataService = dataServiceProvider.get

  val tokens = TableQuery[OAuth2TokensTable]

  def uncompiledFindByLoginInfo(providerId: Rep[String], providerKey: Rep[String]) = tokens.filter(_.providerId === providerId).filter(_.providerKey === providerKey)

  val findByLoginInfoQuery = Compiled(uncompiledFindByLoginInfo _)

  def save(loginInfo: LoginInfo, oauth2Info: OAuth2Info): Future[OAuth2Info] = {
    val expirationTime = oauth2Info.expiresIn.map { seconds => OffsetDateTime.now.plusSeconds(seconds) }
    val maybeSlackScopes = oauth2Info.params.flatMap { paramMap => paramMap.get("scope") }
    val token = OAuth2Token(oauth2Info.accessToken, maybeSlackScopes, oauth2Info.tokenType, expirationTime, oauth2Info.refreshToken, loginInfo)
    save(token).map { _ => oauth2Info }
  }

  def save(token: OAuth2Token): Future[OAuth2Token] = {
    val query = findByLoginInfoQuery(token.loginInfo.providerID, token.loginInfo.providerKey)
    val action = query.result.headOption.flatMap {
      case Some(_) => query.update(token)
      case None => tokens += token
    }.map { _ => token }
    dataService.run(action)
  }

  def deleteToken(token: String): Future[Int] = {
    dataService.run(tokens.filter(_.accessToken === token).delete)
  }

  def deleteLoginInfo(loginInfo: LoginInfo): Future[Unit] = {
    val action = findByLoginInfoQuery(loginInfo.providerID, loginInfo.providerKey).delete.map { _ => () }
    dataService.run(action)
  }

  def findByLoginInfo(loginInfo: LoginInfo): Future[Option[OAuth2Token]] = {
    val action = findByLoginInfoQuery(loginInfo.providerID, loginInfo.providerKey).result.headOption
    dataService.run(action)
  }

}
