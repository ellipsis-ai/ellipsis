package models.behaviors.testing

import models.accounts.user.User
import models.behaviors.events._
import models.team.Team

case class TestMessageEvent(
                      user: User,
                      team: Team,
                      messageText: String,
                      includesBotMention: Boolean
                    ) extends TestEvent with MessageEvent {


}
