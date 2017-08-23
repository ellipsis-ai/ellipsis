package models.environmentvariable

import models.accounts.user.User
import models.behaviors.behaviorversion.BehaviorVersion
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UserEnvironmentVariableService {

  def find(name: String, user: User): Future[Option[UserEnvironmentVariable]]

  def ensureFor(name: String, maybeValue: Option[String], user: User): Future[Option[UserEnvironmentVariable]]

  def deleteFor(name: String, user: User): Future[Boolean]

  def allForAction(user: User): DBIO[Seq[UserEnvironmentVariable]]

  def allFor(user: User): Future[Seq[UserEnvironmentVariable]]

  def lookForInCode(code: String): Seq[String] = {
    // regex quite incomplete, but we're just trying to provide some guidance for now
    """(?s)ellipsis\.userEnv\.([$A-Za-z_][0-9A-Za-z_$]*)""".r.findAllMatchIn(code).flatMap { m =>
      m.subgroups.headOption
    }.toSeq
  }

  def knownUsedIn(behaviorVersion: BehaviorVersion, dataService: DataService): Set[String] = {
    lookForInCode(behaviorVersion.functionBody).toSet
  }

  def missingForAction(user: User, behaviorVersion: BehaviorVersion, dataService: DataService): DBIO[Set[String]] = {
    for {
      envVars <- allForAction(user)
    } yield {
      val usedNames = knownUsedIn(behaviorVersion, dataService)
      val allNames = envVars.filter(_.value.trim.nonEmpty).map(_.name).toSet
      usedNames diff allNames
    }
  }

  def missingFor(user: User, behaviorVersion: BehaviorVersion, dataService: DataService): Future[Set[String]] = {
    dataService.run(missingForAction(user, behaviorVersion, dataService))
  }

}
