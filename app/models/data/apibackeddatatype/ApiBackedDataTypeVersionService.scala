package models.data.apibackeddatatype

import scala.concurrent.Future

trait ApiBackedDataTypeVersionService {

  def find(id: String): Future[Option[ApiBackedDataTypeVersion]]
}
