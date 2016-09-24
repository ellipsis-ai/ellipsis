package models.data.apibackeddatatype

import models.team.{Team, TeamQueries}
import slick.driver.PostgresDriver.api._

object ApiBackedDataTypeQueries {

  val all = TableQuery[ApiBackedDataTypesTable]
  val joined = all.join(TeamQueries.all).on(_.teamId === _.id)

  type TupleType = (RawApiBackedDataType, Team)

  def tuple2DataType(tuple: TupleType): ApiBackedDataType = {
    val raw = tuple._1
    ApiBackedDataType(raw.id, raw.name, tuple._2, raw.maybeCurrentVersionId, raw.maybeImportedId, raw.createdAt)
  }

}
