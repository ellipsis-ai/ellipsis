package models.behaviors.form

import java.time.OffsetDateTime

import Formatting._
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

case class InvalidFormConfigException(json: JsValue) extends Exception {
  override def getMessage: String = Json.prettyPrint(json)
}

case class Form(
                 id: String,
                 config: FormConfig,
                 createdAt: OffsetDateTime
               ) {

  def toRaw: RawForm = {
    RawForm(id, Json.toJson(config), createdAt)
  }
}

object Form {

  def fromRaw(raw: RawForm): Form = {
    raw.config.validate[FormConfig] match {
      case JsSuccess(config, _) => Form(raw.id, config, raw.createdAt)
      case JsError(errors) => throw InvalidFormConfigException(JsError.toJson(errors))
    }

  }

}
