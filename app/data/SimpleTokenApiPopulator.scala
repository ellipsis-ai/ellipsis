package data

import javax.inject._

import models.accounts.simpletokenapi.SimpleTokenApi
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SimpleTokenApiPopulator @Inject() (dataService: DataService) {

  val apis: Seq[SimpleTokenApi] = Seq(
    SimpleTokenApi(
      "pivotal-tracker",
      "Pivotal Tracker",
      Some("https://www.pivotaltracker.com/profile"),
      None
    )
  )

  def run(): Unit = {
    dataService.runNow(Future.sequence(apis.map(dataService.simpleTokenApis.save)).map(_ => {}))
  }

  run()
}
