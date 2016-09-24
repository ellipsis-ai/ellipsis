package models.data.apibackeddatatype

import play.api.libs.json.Json

case class ApiBackedDataTypeValue(id: String, label: String)

object ApiBackedDataTypeValue {
  implicit val dataTypeValueReads = Json.reads[ApiBackedDataTypeValue]
  implicit val dataTypeValueWrites = Json.writes[ApiBackedDataTypeValue]
}
