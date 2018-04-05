package models.devmodechannel

import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import models.team._
import services.DataService
import drivers.SlickPostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

case class DevModeChannel(
                           context: String, // "slack, etc"
                           channel: String,
                           teamId: String,
                           createdAt: OffsetDateTime
                         )

class DevModeChannelsTable(tag: Tag) extends Table[DevModeChannel](tag, "dev_mode_channels") {

  def context = column[String]("context")
  def channel = column[String]("channel")
  def teamId = column[String]("team_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * = (context, channel, teamId, createdAt) <> ((DevModeChannel.apply _).tupled, DevModeChannel.unapply _)
}

class DevModeChannelServiceImpl @Inject() (
                                             dataServiceProvider: Provider[DataService],
                                             implicit val ec: ExecutionContext
                                           ) extends DevModeChannelService {

  def dataService = dataServiceProvider.get

  import DevModeChannelQueries._

  def findAction(context: String, channel: String, team: Team): DBIO[Option[DevModeChannel]] = {
    findQuery(context, channel, team.id).result.map { r =>
      r.headOption
    }
  }

  def find(context: String, channel: String, team: Team): Future[Option[DevModeChannel]] = {
    dataService.run(findAction(context, channel, team))
  }

  def isEnabledForAction(context: String, channel: String, team: Team): DBIO[Boolean] = {
    findAction(context, channel, team).map(_.isDefined)
  }

  def ensureFor(context: String, channel: String, team: Team): Future[DevModeChannel] = {
    val action = findQuery(context, channel, team.id).result.flatMap { r =>
      r.headOption.map(DBIO.successful).getOrElse {
        val newInstance = DevModeChannel(context, channel, team.id, OffsetDateTime.now)
        (all += newInstance).map(_ => newInstance)
      }
    }
    dataService.run(action)
  }

  def delete(devModeChannel: DevModeChannel): Future[Boolean] = {
    val action = findQuery(devModeChannel.context, devModeChannel.channel, devModeChannel.teamId).delete.map { r =>
      r > 0
    }
    dataService.run(action)
  }

}
