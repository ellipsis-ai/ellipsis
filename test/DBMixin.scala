
import com.typesafe.config.ConfigFactory
import models.accounts.SlackProfileQueries
import play.api.db.Databases
import play.api.db.evolutions.Evolutions

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import slick.driver.PostgresDriver.api.{Database => PostgresDatabase}
import slick.driver.PostgresDriver.api._

trait DBMixin {

  lazy val config = ConfigFactory.load()

  def withDatabase[T](fn: PostgresDatabase => T) = {
    Databases.withDatabase(
      driver = config.getString("db.default.driver"),
      url = config.getString("db.default.url"),
      config = Map(
        "user" -> config.getString("db.default.username"),
        "password" -> config.getString("db.default.password")
      )
    ) { database =>
      Evolutions.withEvolutions(database) {
        val db = PostgresDatabase.forConfig("db.default", config)
        fn(db)
        // A misguided legacy down evolution will blow up if any SlackProfiles exist, so delete them
        runNow(db, SlackProfileQueries.profiles.delete)
      }
    }

  }

  def run[T](db: PostgresDatabase, action: DBIO[T]): Future[T] = {
    db.run(action)
  }

  def runNow[T](db: PostgresDatabase, action: DBIO[T]): T = {
    Await.result(run(db, action), 30.seconds)
  }

  def runNow[T](future: Future[T]): T = {
    Await.result(future, 30.seconds)
  }
}
