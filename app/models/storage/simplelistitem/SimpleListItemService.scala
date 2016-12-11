package models.storage.simplelistitem

import models.storage.simplelist.SimpleList
import models.team.Team
import play.api.libs.json.JsValue

import scala.concurrent.Future

trait SimpleListItemService {

  def find(id: String): Future[Option[SimpleListItem]]

  def createFor(list: SimpleList, data: JsValue): Future[SimpleListItem]

  def allFor(list: Team): Future[Seq[SimpleListItem]]

}
