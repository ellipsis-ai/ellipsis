package utils

import play.api.libs.json.{JsString, Writes}

trait Enum[A] {
  trait Value { self: A => }
  val values: List[A]
  def find(name: String) = values.find(_.toString == name)
}

trait JsonEnumValue {
  val value: String
  override def toString: String = value
  implicit val jsonEnumValueWrites: Writes[JsonEnumValue] = new Writes[JsonEnumValue] {
    override def writes(o: JsonEnumValue) = JsString(value)
  }
}

trait JsonEnum[A <: JsonEnumValue] extends Enum[A]
