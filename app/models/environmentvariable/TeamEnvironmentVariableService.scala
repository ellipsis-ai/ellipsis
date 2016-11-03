package models.environmentvariable

import models.behaviors.behaviorversion.BehaviorVersion
import models.team.Team
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TeamEnvironmentVariableService {

  def find(name: String, team: Team): Future[Option[TeamEnvironmentVariable]]

  def ensureFor(name: String, maybeValue: Option[String], team: Team): Future[Option[TeamEnvironmentVariable]]

  def deleteFor(name: String, team: Team): Future[Boolean]

  def allFor(team: Team): Future[Seq[TeamEnvironmentVariable]]

  def lookForInCode(code: String): Seq[String] = {
    // regex quite incomplete, but we're just trying to provide some guidance for now
    """(?s)ellipsis\.env\.([$A-Za-z_][0-9A-Za-z_$]*)""".r.findAllMatchIn(code).flatMap { m =>
      m.subgroups.headOption
    }.toSeq
  }

  def knownUsedIn(behaviorVersion: BehaviorVersion, dataService: DataService): Future[Seq[String]] = {
    dataService.awsConfigs.environmentVariablesUsedFor(behaviorVersion).map { inConfig =>
      inConfig ++ lookForInCode(behaviorVersion.functionBody)
    }
  }

  def missingIn(behaviorVersion: BehaviorVersion, dataService: DataService): Future[Seq[String]] = {
    for {
      envVars <- allFor(behaviorVersion.team)
      missing <- knownUsedIn(behaviorVersion, dataService).map{ used =>
        used diff envVars.filter(_.value.trim.nonEmpty).map(_.name)
      }
    } yield missing
  }

}
