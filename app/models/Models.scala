package models

import javax.inject._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile

@Singleton
class Models @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  def run[T](action: DBIO[T]): Future[T] = {
    db.run(action)
  }

  def runNow[T](action: DBIO[T]): T = {
    runNow(run(action))
  }

  def runNow[T](future: Future[T]): T = {
    Await.result(future, 30.seconds)
  }

}
