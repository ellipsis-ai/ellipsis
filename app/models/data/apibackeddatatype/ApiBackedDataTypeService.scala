package models.data.apibackeddatatype

import models.team.Team

import scala.concurrent.Future

trait ApiBackedDataTypeService {

  //def isValid(text: String, dataType: ApiBackedDataType): Future[Boolean]

  def allFor(team: Team): Future[Seq[ApiBackedDataType]]

}
