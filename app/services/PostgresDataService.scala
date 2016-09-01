package services

import javax.inject._

import models._
import models.accounts._
import slick.dbio.DBIO

import scala.concurrent.Future

@Singleton
class PostgresDataService @Inject() (
                              val models: Models,
                              val userService: UserService,
                              val loginTokenService: LoginTokenService
                            ) extends DataService {

  def run[T](action: DBIO[T]): Future[T] = models.run(action)
}
