package models.environmentvariable

import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TeamEnvironmentVariableService {

  def find(name: String, team: Team): Future[Option[TeamEnvironmentVariable]]

  def ensureFor(name: String, maybeValue: Option[String], team: Team): Future[Option[TeamEnvironmentVariable]]

  def deleteFor(name: String, team: Team): Future[Boolean]

  def allForAction(team: Team): DBIO[Seq[TeamEnvironmentVariable]]

  def allFor(team: Team): Future[Seq[TeamEnvironmentVariable]]

  def lookForInCode(code: String): Seq[String] = {
    // regex quite incomplete, but we're just trying to provide some guidance for now
    """(?s)ellipsis\.env\.([$A-Za-z_][0-9A-Za-z_$]*)""".r.findAllMatchIn(code).flatMap { m =>
      m.subgroups.headOption
    }.toSeq
  }

  def knownUsedInAction(behaviorVersion: BehaviorVersion, dataService: DataService): DBIO[Set[String]] = {
    DBIO.successful(lookForInCode(behaviorVersion.functionBody).toSet)
  }

  def missingInAction(behaviorVersion: BehaviorVersion, dataService: DataService): DBIO[Set[String]] = {
    for {
      envVars <- allForAction(behaviorVersion.team)
      missing <- knownUsedInAction(behaviorVersion, dataService).map { usedNames =>
        val allNames = envVars.filter(_.value.trim.nonEmpty).map(_.name).toSet
        usedNames diff allNames
      }
    } yield missing
  }

}
