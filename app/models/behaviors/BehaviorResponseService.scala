package models.behaviors

import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.conversations.parentconversation.NewParentConversation
import models.behaviors.events.Event
import models.behaviors.triggers.Trigger
import models.team.Team
import slick.dbio.DBIO

import scala.concurrent.Future

trait BehaviorResponseService {

  def parametersWithValuesFor(
                               event: Event,
                               behaviorVersion: BehaviorVersion,
                               paramValues: Map[String, String],
                               maybeConversation: Option[Conversation]
                             ): Future[Seq[ParameterWithValue]]

  def buildForAction(
                      event: Event,
                      behaviorVersion: BehaviorVersion,
                      paramValues: Map[String, String],
                      maybeActivatedTrigger: Option[Trigger],
                      maybeConversation: Option[Conversation],
                      maybeNewParent: Option[NewParentConversation],
                      maybeDialog: Option[DialogInfo],
                      userExpectsResponse: Boolean
                    ): DBIO[BehaviorResponse]

  def buildFor(
                event: Event,
                behaviorVersion: BehaviorVersion,
                paramValues: Map[String, String],
                maybeActivatedTrigger: Option[Trigger],
                maybeConversation: Option[Conversation],
                maybeNewParent: Option[NewParentConversation],
                maybeDialog: Option[DialogInfo],
                userExpectsResponse: Boolean
              ): Future[BehaviorResponse]

  def allFor(
              event: Event,
              maybeTeam: Option[Team],
              maybeLimitToBehavior: Option[Behavior]
            ): Future[Seq[BehaviorResponse]]

}
