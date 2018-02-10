package models.billing.active_user_record


import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import models.accounts.user.User
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

  def create(user: User, createdAt: OffsetDateTime = OffsetDateTime.now): Future[ActiveUserRecord] = {
    save(ActiveUserRecord(IDs.next, user.id, createdAt))
  }

  def save(aur: ActiveUserRecord): Future[ActiveUserRecord] = {
    dataService.run(saveAction(aur))
  }

  def allRecords: Future[Seq[ActiveUserRecord]] = dataService.run(all.result)

  def countFor(teamId: String, start: OffsetDateTime, end: OffsetDateTime): Future[Int] = {
    val action = compiledCountWithTeamIdAndDateQuery(teamId, start, end).result.map(_.length)
    dataService.run(action)
  }


  private def saveAction(aur: ActiveUserRecord): DBIO[ActiveUserRecord] = {
    findQueryFor(aur.id).result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === aur.id).update(aur)
      }.getOrElse {
        all += aur
      }.map { _ => aur }
    }
  }
}
