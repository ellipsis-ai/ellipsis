package models.billing.active_user_record


import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import play.api.Configuration
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class ActiveUserRecordServiceImpl @Inject()(
                                  val dataServiceProvider: Provider[DataService],
                                  configuration: Configuration,
                                  implicit val ec: ExecutionContext
                                ) extends ActiveUserRecordService {

  def dataService = dataServiceProvider.get

  import models.billing.active_user_record.ActiveUserRecordQueries._

  def allRecords: Future[Seq[ActiveUserRecord]] = dataService.run(all.result)

  def countFor(teamId: String, start: OffsetDateTime, end: OffsetDateTime): Future[Int] = {
    dataService.run(uncompiledCountWithTeamIdAndDateQuery(teamId, start, end).result)
  }

}
