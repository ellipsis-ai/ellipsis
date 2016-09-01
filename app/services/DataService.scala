package services

import models.accounts._
import slick.dbio.DBIO

import scala.concurrent.Future

trait DataService {

  val users: UserService
  val loginTokens: LoginTokenService

  def run[T](action: DBIO[T]): Future[T]
}
