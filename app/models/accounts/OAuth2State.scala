package models.accounts

import java.net.URLDecoder

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

  def fromEncodedString(string: String): OAuth2State = {
    val decoded = URLDecoder.decode(string, "utf-8")
    val json = Json.parse(decoded)
    OAuth2State(
      (json \ "oauthState").asOpt[String].get,
      (json \ "invocationId").asOpt[String],
      (json \ "redirect").asOpt[String]
    )
  }

}
