package json

import java.time.OffsetDateTime

import models.accounts.user.{User, UserTeamAccess}
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.events.{MessageEvent, TestEventContext}
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.behaviors.testing.TestMessageEvent
import models.team.Team
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

case class ScheduledActionArgumentData(name: String, value: String)

case class ScheduledActionData(
                                id: Option[String],
                                scheduleType: String,
                                behaviorId: Option[String],
                                behaviorGroupId: Option[String],
                                trigger: Option[String],
                                arguments: Seq[ScheduledActionArgumentData],
                                recurrence: ScheduledActionRecurrenceData,
                                firstRecurrence: Option[OffsetDateTime],
                                secondRecurrence: Option[OffsetDateTime],
                                useDM: Boolean,
                                channel: String,
                                userId: Option[String]
                              )

object ScheduledActionData {
  def fromScheduledMessage(scheduledMessage: ScheduledMessage): ScheduledActionData = {
    ScheduledActionData(
      id = Some(scheduledMessage.id),
      scheduleType = "message",
      behaviorId = None,
      behaviorGroupId = None,
      trigger = Some(scheduledMessage.text),
      arguments = Seq(),
      recurrence = ScheduledActionRecurrenceData.fromRecurrence(scheduledMessage.recurrence),
      firstRecurrence = Some(scheduledMessage.nextSentAt),
      secondRecurrence = scheduledMessage.maybeFollowingSentAt,
      useDM = scheduledMessage.isForIndividualMembers,
      channel = scheduledMessage.maybeChannel.getOrElse(""),
      userId = scheduledMessage.maybeUser.map(_.id)
    )
  }

  def fromScheduledBehavior(scheduledBehavior: ScheduledBehavior): ScheduledActionData = {
    val arguments = scheduledBehavior.arguments.map { case (key, value) => ScheduledActionArgumentData(key, value) }.toSeq
    ScheduledActionData(
      id = Some(scheduledBehavior.id),
      scheduleType = "behavior",
      behaviorId = Some(scheduledBehavior.behavior.id),
      behaviorGroupId = Some(scheduledBehavior.behavior.group.id),
      trigger = None,
      arguments = arguments,
      recurrence = ScheduledActionRecurrenceData.fromRecurrence(scheduledBehavior.recurrence),
      firstRecurrence = Some(scheduledBehavior.nextSentAt),
      secondRecurrence = scheduledBehavior.maybeFollowingSentAt,
      useDM = scheduledBehavior.isForIndividualMembers,
      channel = scheduledBehavior.maybeChannel.getOrElse(""),
      userId = scheduledBehavior.maybeUser.map(_.id)
    )
  }

  def buildForAdmin(team: Team, dataService: DataService)(implicit ec: ExecutionContext): Future[Seq[ScheduledActionData]] = {
    for {
      scheduledBehaviors <- dataService.scheduledBehaviors.allActiveForTeam(team)
      scheduledMessages <- dataService.scheduledMessages.allForTeam(team)
    } yield {
      val scheduledMessageData = scheduledMessages.map(ScheduledActionData.fromScheduledMessage)
      val scheduledBehaviorData = scheduledBehaviors.map(ScheduledActionData.fromScheduledBehavior)
      scheduledMessageData ++ scheduledBehaviorData
    }
  }

  def buildForUserTeamAccess(
                              team: Team,
                              teamAccess: UserTeamAccess,
                              dataService: DataService,
                              teamChannelsData: Seq[TeamChannelsData],
                              maybeSlackUserId: Option[String],
                              forceAdmin: Boolean
                            )(implicit ec: ExecutionContext): Future[Seq[ScheduledActionData]] = {
    buildForAdmin(team, dataService).map { allScheduledActions =>
      if (teamAccess.isAdminAccess || forceAdmin) {
        allScheduledActions
      } else {
        val convoIds = teamChannelsData.flatMap(_.channelList.map(_.id))
        allScheduledActions.filter { action =>
          action.userId.contains(teamAccess.user.id) || convoIds.contains(action.channel)
        }
      }
    }
  }

  def buildFor(group: BehaviorGroup, user: User, dataService: DataService)(implicit ec: ExecutionContext): Future[Seq[ScheduledActionData]] = {
    for {
      behaviors <- dataService.behaviors.allForGroup(group)
      scheduledBehaviors <- Future.sequence(behaviors.map(ea => dataService.scheduledBehaviors.allForBehavior(ea, None, None))).map(_.flatten)
      allScheduledMessages <- dataService.scheduledMessages.allForTeam(group.team)
      groupTriggers <- dataService.triggers.allActiveFor(group)
    } yield {
      val scheduledMessages = allScheduledMessages.filter { sm =>
        val text = MessageEvent.ellipsisShortcutMentionRegex.replaceFirstIn(sm.text, "")
        val testEvent = TestMessageEvent(TestEventContext(user, group.team), text, includesBotMention = true, None)
        groupTriggers.exists(_.isActivatedBy(testEvent))
      }
      scheduledBehaviors.map(ScheduledActionData.fromScheduledBehavior) ++
        scheduledMessages.map(ScheduledActionData.fromScheduledMessage)
    }
  }
}
