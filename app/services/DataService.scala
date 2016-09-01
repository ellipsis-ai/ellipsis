package services

import models.accounts._
import slick.dbio.DBIO

import scala.concurrent.Future

trait DataService {

  val userService: UserService
  val loginTokenService: LoginTokenService

  def run[T](action: DBIO[T]): Future[T]
}
