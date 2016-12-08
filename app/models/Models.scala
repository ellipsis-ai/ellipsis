package models

import javax.inject._

import play.api.inject.ApplicationLifecycle

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import drivers.SlickPostgresDriver.api._

@Singleton
class Models @Inject() (appLifecycle: ApplicationLifecycle) {

  var db: Database = Database.forConfig("db.default")

  appLifecycle.addStopHook { () =>
    if (db != null) {
      println("closing DB pool")
      db.close()
      db = null
    }
    Future.successful(())
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
    runNow(run(action))
  }

  def runNow[T](future: Future[T]): T = {
    Await.result(future, 30.seconds)
  }
}
