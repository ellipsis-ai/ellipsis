package models.behaviors.invocationtoken

import models.team.Team
import scala.concurrent.Future

trait InvocationTokenService {

  def find(id: String): Future[Option[InvocationToken]]

  def createFor(team: Team): Future[InvocationToken]

  def use(token: InvocationToken): Future[InvocationToken]

}
