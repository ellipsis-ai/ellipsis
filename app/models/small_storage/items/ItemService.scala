package models.small_storage.items

import models.team.Team

import scala.concurrent.Future

/**
  * Created by matteo on 12/7/16.
  */
trait ItemService {

  def create(team: Team, kind: String, data: String): Future[Boolean]

  def save(item: Item): Future[Boolean]
}
