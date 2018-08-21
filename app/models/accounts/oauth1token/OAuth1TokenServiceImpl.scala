package models.accounts.oauth1token

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import drivers.SlickPostgresDriver.api._
import javax.inject.{Inject, Provider}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class OAuth1TokensTable(tag: Tag) extends Table[OAuth1Token](tag, "oauth_1_tokens") {
  def token = column[String]("token")
  def secret = column[String]("secret")
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")

  def loginInfo = (providerId, providerKey) <> (LoginInfo.tupled, LoginInfo.unapply _)

  def pk = primaryKey("oauth_2_tokens_pkey", (providerId, providerKey))

  def * = (token, secret, loginInfo) <> ((OAuth1Token.apply _).tupled, OAuth1Token.unapply _)
}

class OAuth1TokenServiceImpl @Inject() (
                                         dataServiceProvider: Provider[DataService],
                                         implicit val ec: ExecutionContext
                                       ) extends OAuth1TokenService {

  def dataService = dataServiceProvider.get

  val tokens = TableQuery[OAuth1TokensTable]

  def uncompiledFindByLoginInfo(providerId: Rep[String], providerKey: Rep[String]) = tokens.filter(_.providerId === providerId).filter(_.providerKey === providerKey)

  val findByLoginInfoQuery = Compiled(uncompiledFindByLoginInfo _)

  def save(loginInfo: LoginInfo, oauth1Info: OAuth1Info): Future[OAuth1Info] = {
    val token = OAuth1Token(oauth1Info.token, oauth1Info.secret, loginInfo)
    save(token).map { _ => oauth1Info }
  }

  def save(token: OAuth1Token): Future[OAuth1Token] = {
    val query = findByLoginInfoQuery(token.loginInfo.providerID, token.loginInfo.providerKey)
    val action = query.result.headOption.flatMap {
      case Some(_) => query.update(token)
      case None => tokens += token
    }.map { _ => token }
    dataService.run(action)
  }

  def deleteToken(token: String): Future[Int] = {
    dataService.run(tokens.filter(_.token === token).delete)
  }

  def deleteLoginInfo(loginInfo: LoginInfo): Future[Unit] = {
    val action = findByLoginInfoQuery(loginInfo.providerID, loginInfo.providerKey).delete.map { _ => () }
    dataService.run(action)
  }

  def findByLoginInfo(loginInfo: LoginInfo): Future[Option[OAuth1Token]] = {
    val action = findByLoginInfoQuery(loginInfo.providerID, loginInfo.providerKey).result.headOption
    dataService.run(action)
  }

}
