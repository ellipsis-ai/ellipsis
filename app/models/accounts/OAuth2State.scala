package models.accounts

import java.net.URLDecoder

import models.IDs
import play.api.libs.json.{JsObject, Json}

case class OAuth2State(
                        id: String,
                        maybeInvocationId: Option[String],
                        maybeRedirectAfterAuth: Option[String]
                      ) {

  def unencodedString: String = {
    val data = Seq(
      Some("oauthState" -> Json.toJson(id)),
      maybeInvocationId.map(v => "invocationId" -> Json.toJson(v)),
      maybeRedirectAfterAuth.map(v => "redirect" -> Json.toJson(v))
    ).flatten
    JsObject(data).toString
  }

  def encodedString: String = java.net.URLEncoder.encode(unencodedString, "utf-8")

}

object OAuth2State {

  def maybeFromEncodedString(string: String): Option[OAuth2State] = {
    val decoded = URLDecoder.decode(string, "utf-8")
    val json = Json.parse(decoded)
    (json \ "oauthState").asOpt[String].map { id =>
      OAuth2State(
        id,
        (json \ "invocationId").asOpt[String],
        (json \ "redirect").asOpt[String]
      )
    }
  }

  def ensureFor(maybeEncodedString: Option[String]): OAuth2State = {
    maybeEncodedString.flatMap(maybeFromEncodedString).getOrElse {
      OAuth2State(IDs.next, None, None)
    }
  }

}
