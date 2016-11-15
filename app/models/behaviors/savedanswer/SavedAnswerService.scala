package models.behaviors.savedanswer

import models.accounts.user.User
import models.behaviors.behaviorparameter.BehaviorParameter
import models.behaviors.input.Input

import scala.concurrent.Future

trait SavedAnswerService {

  def find(input: Input, user: User): Future[Option[SavedAnswer]]

  def ensureFor(input: Input, valueString: String, user: User): Future[SavedAnswer]

  def allFor(user: User, params: Seq[BehaviorParameter]): Future[Seq[SavedAnswer]]

}
