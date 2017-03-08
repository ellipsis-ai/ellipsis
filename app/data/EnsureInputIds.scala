package data

import javax.inject._

import drivers.SlickPostgresDriver.api._
import models.IDs
import models.behaviors.input.InputQueries
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnsureInputIds @Inject() (dataService: DataService) {

  def run(): Unit = {
    dataService.runNow(
      dataService.run(InputQueries.all.result).flatMap { inputs =>
        Future.sequence(inputs.map { ea =>
          val newInputId = IDs.next
          dataService.run(InputQueries.all.filter(_.id === ea.id).map(_.maybeInputId).update(Some(newInputId)))
        })
      }.map(_ => {})
    )
  }

  run()
}
