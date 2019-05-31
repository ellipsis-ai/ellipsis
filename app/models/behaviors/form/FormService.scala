package models.behaviors.form

import scala.concurrent.Future

trait FormService {

  def create(config: FormConfig, createdFromBehaviorGroupVersionId: String): Future[Form]

  def find(id: String): Future[Option[Form]]

}
