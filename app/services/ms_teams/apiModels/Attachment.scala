package services.ms_teams.apiModels

import play.api.libs.json.JsValue

sealed trait Attachment {
  val contentType: String
  val name: String
  val thumbnailUrl: Option[String]
}

case class LinkAttachment(
                       contentType: String,
                       contentUrl: String,
                       name: String,
                       thumbnailUrl: Option[String]
                     ) extends Attachment

case class ContentAttachment(
                           contentType: String,
                           content: JsValue,
                           name: String,
                           thumbnailUrl: Option[String]
                         ) extends Attachment
