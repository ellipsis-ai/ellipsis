package models.small_storage.items

import models.accounts.user.User
import models.team.Team

import scala.concurrent.Future

/**
  * Created by matteo on 12/7/16.
  */
trait ItemService {

//  def find(id: String): Future[Option[Item]]
//
//  def all(team: Team): Future[Seq[Item]]
//
    def create(team: Team, kind: String, data: String): Future[Item]
//
//  def save(item: Item): Future[Item]
}
