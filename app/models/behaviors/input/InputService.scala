package models.behaviors.input

import json.InputData
import models.team.Team

import scala.concurrent.Future

trait InputService {

//  def maybeFor(behaviorParameter: BehaviorParameter): Future[Option[Input]]

  def ensureFor(data: InputData, team: Team): Future[Input]

}
