package models.small_storage.items

import models.team.Team
import play.api.libs.json.JsObject

import scala.concurrent.Future


//
trait ItemService {

  def create(team: Team, kind: String, data: JsObject): Future[Item]

  def save(item: Item): Future[Item]

  def find(itemId: String): Future[Option[Item]]

  def delete(itemId: String): Future[Option[Item]]

  def allForTeam(team: Team): Future[Seq[Item]]

  def count(team: Team): Future[Int]

}
