package models.behaviors.behaviorversion

import json.BehaviorVersionData
import models.accounts.user.User
import models.behaviors.{BehaviorResult, ParameterWithValue}
import models.behaviors.behavior.Behavior
import models.behaviors.events.MessageEvent
import models.environmentvariable.EnvironmentVariable

import scala.concurrent.Future

trait BehaviorVersionService {

  def currentIdsWithFunction: Future[Seq[String]]

  def allFor(behavior: Behavior): Future[Seq[BehaviorVersion]]

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorVersion]]

  def createFor(behavior: Behavior, maybeUser: Option[User]): Future[BehaviorVersion]

  def createFor(
                 behavior: Behavior,
                 maybeUser: Option[User],
                 data: BehaviorVersionData
               ): Future[BehaviorVersion]

  def save(behaviorVersion: BehaviorVersion): Future[BehaviorVersion]

  def delete(behaviorVersion: BehaviorVersion): Future[BehaviorVersion]

  def environmentVariablesUsedInCode(functionBody: String): Seq[String]

  def missingEnvironmentVariablesIn(
                                     behaviorVersion: BehaviorVersion,
                                     environmentVariables: Seq[EnvironmentVariable]
                                   ): Future[Seq[String]]

  def maybeFunctionFor(behaviorVersion: BehaviorVersion): Future[Option[String]]

  def resultFor(
                 behaviorVersion: BehaviorVersion,
                 parametersWithValues: Seq[ParameterWithValue],
                 event: MessageEvent
               ): Future[BehaviorResult]

  def unlearn(behaviorVersion: BehaviorVersion): Future[Unit]

  def redeploy(behaviorVersion: BehaviorVersion): Future[Unit]

}
