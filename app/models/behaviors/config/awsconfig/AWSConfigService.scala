package models.behaviors.config.awsconfig

import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait AWSConfigService {

  def allFor(team: Team): Future[Seq[AWSConfig]]

  def findAction(id: String): DBIO[Option[AWSConfig]]

  def find(id: String): Future[Option[AWSConfig]]

  def save(config: AWSConfig): Future[AWSConfig]
}
