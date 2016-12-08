package data

import javax.inject._

import models.IDs
import models.behaviors.behavior.BehaviorQueries
import models.behaviors.behaviorgroup.{BehaviorGroupQueries, RawBehaviorGroup}
import org.joda.time.DateTime
import services.DataService
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnsureGroups @Inject() (dataService: DataService) {

  def run(): Unit = {
    dataService.runNow(for {
      behaviors <- dataService.run(BehaviorQueries.all.result)
      _ <- Future.sequence(behaviors.map { ea =>
        if (ea.groupId.isEmpty) {
          val raw = RawBehaviorGroup(IDs.next, "", None, None, ea.teamId, DateTime.now)
          dataService.run((BehaviorGroupQueries.all += raw).andThen {
            BehaviorQueries.all.filter(_.id === ea.id).map(_.groupId).update(Some(raw.id))
          })
        } else {
          Future.successful({})
        }
      })
    } yield {})
  }

  run()
}
