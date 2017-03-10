package data

import javax.inject._

import drivers.SlickPostgresDriver.api._
import models.IDs
import models.behaviors.input.{InputQueries, RawInput}
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnsureInputIds @Inject() (dataService: DataService) {

  def runSequentiallyForInputs(inputs: List[RawInput]): Future[Unit] = {
    inputs.headOption.map { input =>
      val newInputId = IDs.next
      dataService.run(InputQueries.all.filter(_.id === input.id).map(_.maybeInputId).update(Some(newInputId))).flatMap { _ =>
        runSequentiallyForInputs(inputs.tail)
      }
    }.getOrElse {
      Future.successful({})
    }
  }

  def run(): Unit = {
    dataService.runNow(
      dataService.run(InputQueries.all.result).flatMap { inputs =>
        runSequentiallyForInputs(inputs.toList)
      }.map(_ => {})
    )
  }

  run()
}
