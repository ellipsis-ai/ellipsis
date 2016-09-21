package models.environmentvariable

import models.team.Team

import scala.concurrent.Future

trait EnvironmentVariableService {

  def find(name: String, team: Team): Future[Option[EnvironmentVariable]]

  def ensureFor(name: String, maybeValue: Option[String], team: Team): Future[Option[EnvironmentVariable]]

  def deleteFor(name: String, team: Team): Future[Boolean]

  def allFor(team: Team): Future[Seq[EnvironmentVariable]]

  def lookForInCode(code: String): Seq[String] = {
    // regex quite incomplete, but we're just trying to provide some guidance for now
    """(?s)ellipsis\.env\.([$A-Za-z_][0-9A-Za-z_$]*)""".r.findAllMatchIn(code).flatMap { m =>
      m.subgroups.headOption
    }.toSeq
  }

}
