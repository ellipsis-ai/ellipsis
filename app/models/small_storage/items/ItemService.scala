package models.small_storage.items

import models.team.Team
import play.api.libs.json.JsObject

import scala.concurrent.Future


trait ItemService {

  // Each team can store at 10,000 items max for now.
  // TODO: Make this configurable, either make it a DB Teams attribute or a record in ES
  val maxItemCount: Int = 10000

  def create_and_add(team: Team, kind: String, data: JsObject): Future[Item]

  def add(item: Item): Future[Item]

  def find(itemId: String): Future[Option[Item]]

  def remove(itemId: String): Future[Option[Item]]

  def countForTeam(team: Team): Future[Int]

  def allForTeam(team: Team): Future[Seq[Item]]

}
