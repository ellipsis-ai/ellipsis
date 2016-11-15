package models.behaviors.input

import json.InputData
import models.team.Team

import scala.concurrent.Future

trait InputService {

  def ensureFor(data: InputData, team: Team): Future[Input]

}
