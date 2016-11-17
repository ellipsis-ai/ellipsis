package data

import javax.inject._

import models.IDs
import models.behaviors.behaviorparameter.BehaviorParameterQueries
import models.behaviors.input.{InputQueries, RawInput}
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnsureInputs @Inject() (dataService: DataService) {

  def run(): Unit = {
    dataService.runNow(for {
      params <- dataService.run(BehaviorParameterQueries.all.result)
      _ <- Future.sequence(params.map { ea =>
        if (ea.inputId.isEmpty) {
          val raw = RawInput(IDs.next, ea.name, ea.maybeQuestion, ea.paramType, isSavedForTeam = false, isSavedForUser = false, None)
          dataService.run((InputQueries.all += raw).andThen {
            BehaviorParameterQueries.all.filter(_.id === ea.id).map(_.inputId).update(Some(raw.id))
          })
        } else {
          Future.successful({})
        }
      })
    } yield {})
  }

  run()
}
