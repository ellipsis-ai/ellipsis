package models.small_storage.items

import models.team.Team
import play.api.libs.json.JsObject

import scala.concurrent.Future


trait ItemService {

  def create(team: Team, kind: String, data: JsObject): Future[Item]

  def save(item: Item): Future[Item]
}
