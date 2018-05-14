package json

import java.time.OffsetDateTime

import models.accounts.user.UserTeamAccess
import models.behaviors.scheduling.scheduledbehavior.ScheduledBehavior
import models.behaviors.scheduling.scheduledmessage.ScheduledMessage
import models.team.Team
import services.DataService
import utils.ChannelLike

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
                              ) {
  def visibleToSlackUser(slackUserId: String, channelList: Seq[ChannelLike]): Boolean = {
    channelList.exists { someChannel =>
      someChannel.id == channel && someChannel.visibleToUser(slackUserId)
    }
  }
}

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
      secondRecurrence = Some(scheduledMessage.followingSentAt),
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
      secondRecurrence = Some(scheduledBehavior.followingSentAt),
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
                              maybeChannelList: Option[Seq[ChannelLike]],
                              maybeSlackUserId: Option[String],
                              forceAdmin: Boolean
                            )(implicit ec: ExecutionContext): Future[Seq[ScheduledActionData]] = {
    for {
      allScheduledActions <- buildForAdmin(team, dataService)
    } yield {
      if (teamAccess.isAdminAccess || forceAdmin) {
        allScheduledActions
      } else {
        (for {
          channelList <- maybeChannelList
          slackUserId <- maybeSlackUserId
        } yield {
          allScheduledActions.filter(_.visibleToSlackUser(slackUserId, channelList))
        }).getOrElse {
          Seq()
        }
      }
    }
  }
}
