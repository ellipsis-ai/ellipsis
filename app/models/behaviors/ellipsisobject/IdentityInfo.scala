package models.behaviors.ellipsisobject

import models.accounts.linkedaccount.LinkedAccount
import models.accounts.linkedoauth1token.LinkedOAuth1Token
import models.accounts.linkedoauth2token.LinkedOAuth2Token
import models.accounts.linkedsimpletoken.LinkedSimpleToken
import models.accounts.user.User
import play.api.libs.json._
import services.DefaultServices
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class IdentityInfo(
                         platformName: String,
                         integrationName: Option[String],
                         userIdOnPlatform: Option[String],
                         accessToken: Option[String]
                       ) {

  def toJson: JsObject = {
    val idPart = userIdOnPlatform.map(v => "id" -> JsString(v)).toSeq
    val tokenPart = accessToken.map { v =>
      Seq("token" -> JsString(v), "oauthToken" -> JsString(v))
    }.toSeq.flatten
    val integrationPart = integrationName.map(v => "integration" -> JsString(v)).toSeq
    JsObject(Seq(
      "externalSystem" -> JsString(platformName), // TODO: deprecated
      "platform" -> JsString(platformName)
    ) ++ integrationPart ++ tokenPart ++ idPart)
  }

}

object IdentityInfo {

  def forOAuth1Token(token: LinkedOAuth1Token): IdentityInfo = {
    IdentityInfo(
      token.application.api.name,
      Some(token.application.name),
      None,
      Some(token.accessToken)
    )
  }

  def forOAuth2Token(token: LinkedOAuth2Token): IdentityInfo = {
    IdentityInfo(
      token.application.api.name,
      Some(token.application.name),
      None,
      Some(token.accessToken)
    )
  }

  def forSimpleToken(token: LinkedSimpleToken): IdentityInfo = {
    IdentityInfo(
      token.api.name,
      None,
      None,
      Some(token.accessToken)
    )
  }

  def forLinkedAccount(linked: LinkedAccount): IdentityInfo = {
    IdentityInfo(
      linked.loginInfo.providerID,
      None,
      Some(linked.loginInfo.providerKey),
      accessToken = None
    )
  }

  def allForAction(user: User, services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Seq[IdentityInfo]] = {
    for {
      linkedOAuth1Tokens <- services.dataService.linkedOAuth1Tokens.allForUserAction(user, services.ws)
      linkedOAuth2Tokens <- services.dataService.linkedOAuth2Tokens.allForUserAction(user, services.ws)
      linkedSimpleTokens <- services.dataService.linkedSimpleTokens.allForUserAction(user)
      linkedAccounts <- services.dataService.linkedAccounts.allForAction(user)
    } yield {
      linkedOAuth1Tokens.map(IdentityInfo.forOAuth1Token) ++
        linkedOAuth2Tokens.map(IdentityInfo.forOAuth2Token) ++
        linkedSimpleTokens.map(IdentityInfo.forSimpleToken) ++
        linkedAccounts.map(IdentityInfo.forLinkedAccount)
    }
  }

}
