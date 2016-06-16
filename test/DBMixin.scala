
import com.typesafe.config.ConfigFactory
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
        fn(PostgresDatabase.forConfig("db.default", config))
      }
    }

  }

  def run[T](db: PostgresDatabase, action: DBIO[T]): Future[T] = {
    db.run(action)
  }

  def runNow[T](db: PostgresDatabase, action: DBIO[T]): T = {
    Await.result(run(db, action), 30.seconds)
  }
}
