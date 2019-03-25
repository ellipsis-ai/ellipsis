package models.behaviors.testing

import json.UserData
import models.accounts.user.User
import models.behaviors.events._
import models.team.Team
import services.DefaultServices
import slick.dbio.DBIO
import utils.FileReference

import scala.concurrent.ExecutionContext

case class TestMessageEvent(
                            eventContext: TestEventContext,
                            messageText: String,
                            includesBotMention: Boolean
                          ) extends TestEvent with MessageEvent {

  override type EC = TestEventContext

  val user: User = eventContext.user
  val team: Team = eventContext.team

  def messageUserDataListAction(services: DefaultServices)(implicit ec: ExecutionContext): DBIO[Set[UserData]] = DBIO.successful(Set.empty)

  val isResponseExpected = true

  val maybeMessageIdForReaction: Option[String] = None

  val maybeFile: Option[FileReference] = None

}
