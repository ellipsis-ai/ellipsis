package models.devmodechannel

import models.team.Team

import scala.concurrent.Future

trait DevModeChannelService {

  def find(context: String, channel: String, team: Team): Future[Option[DevModeChannel]]

  def isEnabledFor(context: String, channel: String, team: Team): Future[Boolean]

  def ensureFor(context: String, channel: String, team: Team): Future[DevModeChannel]

  def delete(devModeChannel: DevModeChannel): Future[Boolean]

}
