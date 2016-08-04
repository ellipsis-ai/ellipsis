package models.accounts

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.exceptions.ProfileRetrievalException
import com.mohiva.play.silhouette.impl.providers.SocialProfileParser
import play.api.libs.json._

import scala.concurrent.Future

case class CustomProfileParseException(json: JsValue, message: String) extends Exception(message)

class CustomProfileParser(val authConfiguration: CustomOAuth2Configuration) extends SocialProfileParser[JsValue, CustomProfile] {

  def lookup(json: JsValue, pathElements: Seq[String]): Option[JsValue] = {
    if (pathElements.isEmpty) {
      Some(json)
    } else {
      (json \ pathElements.head).toOption.flatMap { found =>
        lookup(found, pathElements.tail)
      }
    }
  }

  def parse(json: JsValue): Future[CustomProfile] = Future.successful {
    lookup(json, authConfiguration.getProfilePathElements).map { found =>
      CustomProfile(LoginInfo(authConfiguration.name, found.toString))
    }.getOrElse {
      val message = "Couldn't retrieve user info"
      throw new ProfileRetrievalException(message, CustomProfileParseException(json, message))
    }
  }

}
