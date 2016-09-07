package models.environmentvariable

import models.team.Team

import scala.concurrent.Future

trait EnvironmentVariableService {

  def find(name: String, team: Team): Future[Option[EnvironmentVariable]]

  def ensureFor(name: String, maybeValue: Option[String], team: Team): Future[Option[EnvironmentVariable]]

  def deleteFor(name: String, team: Team): Future[Boolean]

  def allFor(team: Team): Future[Seq[EnvironmentVariable]]

}
