package models.environmentvariable

import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UserEnvironmentVariableService {

  def find(name: String, user: User): Future[Option[UserEnvironmentVariable]]

  def ensureFor(name: String, maybeValue: Option[String], user: User): Future[Option[UserEnvironmentVariable]]

  def deleteFor(name: String, user: User): Future[Boolean]

  def allFor(user: User): Future[Seq[UserEnvironmentVariable]]

  def lookForInCode(code: String): Seq[String] = {
    // regex quite incomplete, but we're just trying to provide some guidance for now
    """(?s)ellipsis\.userEnv\.([$A-Za-z_][0-9A-Za-z_$]*)""".r.findAllMatchIn(code).flatMap { m =>
      m.subgroups.headOption
    }.toSeq
  }

  def knownUsedIn(behaviorVersion: BehaviorVersion, dataService: DataService): Future[Seq[String]] = {
    Future.successful(lookForInCode(behaviorVersion.functionBody))
  }

  def missingFor(user: User, behaviorVersion: BehaviorVersion, dataService: DataService): Future[Seq[String]] = {
    for {
      envVars <- allFor(user)
      missing <- knownUsedIn(behaviorVersion, dataService).map{ used =>
        used diff envVars.filter(_.value.trim.nonEmpty).map(_.name)
      }
    } yield missing
  }

}
