package models

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import slick.driver.PostgresDriver.api._

object Models {

  var db: Database = null

  def onStop(): Unit = {
    if (db != null) {
      println("closing DB pool")
      db.close()
      db = null
    }
  }

  def onStart(): Unit = {
    println("initializing DB pool")
    onStop()
    db = Database.forConfig("db.default")
  }

  def withDatabase[T](fn: Database => Future[T]) = {
    fn(db)
  }

  def run[T](action: DBIO[T]): Future[T] = {
    withDatabase { db =>
      db.run(action)
    }
  }

  def runNow[T](action: DBIO[T]): T = {
    Await.result(Models.run(action), 30.seconds)
  }
}
