package support

import com.typesafe.config.ConfigFactory
import play.api.db.Databases
import play.api.db.evolutions.Evolutions
import services.PostgresDataService
import slick.driver.PostgresDriver.api.{Database => PostgresDatabase, _}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait DBMixin {

  lazy val config = ConfigFactory.load()

  def withEmptyDB[T](dataService: PostgresDataService, fn: PostgresDatabase => T) = {
    Databases.withDatabase(
      driver = config.getString("db.default.driver"),
      url = config.getString("db.default.url"),
      config = Map(
        "username" -> config.getString("db.default.username"),
        "password" -> config.getString("db.default.password")
      )
    ) { database =>
      Evolutions.withEvolutions(database) {
        fn(dataService.models.db)
        // A misguided legacy down evolution will blow up if any SlackProfiles exist, so delete them
        runNow(dataService.slackProfiles.deleteAll())
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
