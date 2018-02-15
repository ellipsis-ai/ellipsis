package models.behaviors.behaviorparameter

import json.Formatting._
import models.behaviors.SuccessResult
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.datatypefield.DataTypeField
import models.behaviors.defaultstorageitem.DefaultStorageItem
import play.api.libs.json._

case class DataTypeResultBody(values: Seq[ValidValue]) {

  private def textMatchesLabel(text: String, label: String, context: BehaviorParameterContext): Boolean = {
    text.toLowerCase == label.toLowerCase
  }

  def maybeValueAtIndex(index: Int): Option[ValidValue] = values.lift(index)

  def maybeValueForId(id: String): Option[ValidValue] = values.find(_.id == id)

  def maybeValueForText(text: String, context: BehaviorParameterContext): Option[ValidValue] = {
    values.find {
      v => v.id == text || textMatchesLabel(text, v.label, context)
    }
  }

  def isEmpty: Boolean = values.isEmpty

}

object DataTypeResultBody {

  def empty = DataTypeResultBody(Seq())

  def fromSuccessResult(result: SuccessResult): DataTypeResultBody = {
    val json = result.result
    json.asOpt[DataTypeResultBody].getOrElse {
      json.asOpt[Seq[ValidValue]].map(DataTypeResultBody.apply).getOrElse {
        json.asOpt[Seq[String]].map(strings => DataTypeResultBody(strings.map { ea => ValidValue(ea, ea, Map()) })).getOrElse {
          empty
        }
      }
    }
  }

  def fromDefaultStorageItems(
                               items: Seq[DefaultStorageItem],
                               maybeLabelField: Option[DataTypeField],
                               behaviorVersion: BehaviorVersion
                             ): DataTypeResultBody = {
    val values = items.map(_.data).flatMap {
      case ea: JsObject =>
        val maybeLabel = maybeLabelField.flatMap { labelField => (ea \ labelField.name).asOpt[String] }
        Json.toJson(Map(
          "id" -> JsString(behaviorVersion.id),
          "label" -> maybeLabel.map(JsString.apply).getOrElse(JsNull)
        ) ++ ea.value).asOpt[ValidValue]
      case _ => None
    }
    DataTypeResultBody(values)
  }
}
