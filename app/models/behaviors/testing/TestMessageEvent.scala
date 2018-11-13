package models.behaviors.testing

import models.accounts.user.User
import models.behaviors.events._
import models.team.Team

case class TestMessageEvent(
                            eventContext: TestEventContext,
                            messageText: String,
                            includesBotMention: Boolean
                          ) extends TestEvent with MessageEvent {

  override type EC = TestEventContext

  val user: User = eventContext.user
  val team: Team = eventContext.team

  def messageUserDataList: Set[MessageUserData] = Set.empty

  val isResponseExpected = true

  val maybeMessageIdForReaction: Option[String] = None

}
