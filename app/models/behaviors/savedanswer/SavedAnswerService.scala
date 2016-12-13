package models.behaviors.savedanswer

import models.accounts.user.User
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.input.Input

import scala.concurrent.Future

trait SavedAnswerService {

  def find(input: Input, user: User): Future[Option[SavedAnswer]]

  def ensureFor(input: Input, valueString: String, user: User): Future[SavedAnswer]

  def allFor(user: User, params: Seq[BehaviorParameter]): Future[Seq[SavedAnswer]]

  def allFor(input: Input): Future[Seq[SavedAnswer]]

  def updateForInputId(maybeOldInputId: Option[String], newInputId: String): Future[Unit]

  def deleteForUser(input: Input, user: User): Future[Int]

  def deleteAllFor(input: Input): Future[Int]

}
