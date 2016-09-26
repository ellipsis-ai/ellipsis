package models.data.apibackeddatatype

import play.api.libs.json.Json
import slick.driver.PostgresDriver.api._

object ApiBackedDataTypeVersionQueries {

  val all = TableQuery[ApiBackedDataTypeVersionsTable]
  val joined = all.join(ApiBackedDataTypeQueries.joined).on(_.dataTypeId === _._1.id)

  type TupleType = (RawApiBackedDataTypeVersion, ApiBackedDataTypeQueries.TupleType)

  def tuple2DataType(tuple: TupleType): ApiBackedDataTypeVersion = {
    val raw = tuple._1
    val dataType = ApiBackedDataTypeQueries.tuple2DataType(tuple._2)
    ApiBackedDataTypeVersion(
      raw.id,
      raw.name,
      dataType,
      raw.maybeHttpMethod,
      raw.maybeUrl,
      raw.maybeRequestBody.map(Json.parse),
      raw.maybeFunctionBody,
      raw.createdAt
    )
  }

}
