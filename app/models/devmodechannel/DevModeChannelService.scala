package models.devmodechannel

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.Event
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait DevModeChannelService {

  def findAction(context: String, channel: String, team: Team): DBIO[Option[DevModeChannel]]

  def find(context: String, channel: String, team: Team): Future[Option[DevModeChannel]]

  def isEnabledForAction(event: Event, behaviorVersion: BehaviorVersion): DBIO[Boolean]

  def ensureFor(context: String, channel: String, team: Team): Future[DevModeChannel]

  def delete(devModeChannel: DevModeChannel): Future[Boolean]

}
