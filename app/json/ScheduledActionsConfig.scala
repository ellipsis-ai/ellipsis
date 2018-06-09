package json

import java.time.format.TextStyle
import java.util.Locale

import akka.actor.ActorSystem
import models.accounts.user.{User, UserTeamAccess}
import services.DefaultServices
import services.slack.SlackApiError
import utils.{SlackChannels, SlackConversation}

import scala.concurrent.{ExecutionContext, Future}

case class ScheduledActionsConfig(
                                   containerId: String,
                                   csrfToken: Option[String],
                                   teamId: String,
                                   scheduledActions: Seq[ScheduledActionData],
                                   channelList: Option[Seq[ScheduleChannelData]],
                                   behaviorGroups: Seq[BehaviorGroupData],
                                   teamTimeZone: Option[String],
                                   teamTimeZoneName: Option[String],
                                   slackUserId: Option[String],
                                   slackBotUserId: Option[String],
                                   selectedScheduleId: Option[String],
                                   newAction: Option[Boolean],
                                   isAdmin: Boolean
                                 )

object ScheduledActionsConfig {

  private def maybeIsPrivateMemberFor(convo: SlackConversation, slackUserId: String, channels: SlackChannels)(implicit ec: ExecutionContext): Future[Option[Boolean]] = {
    if (convo.isIm || convo.isMpim || convo.isPrivateChannel) {
      channels.getMembersFor(convo.id).map { members =>
        Some(members.contains(slackUserId))
      }
    } else {
      Future.successful(None)
    }
  }

  private def maybeChannelDataFor(
                                   convo: SlackConversation,
                                   slackUserId: String,
                                   channels: SlackChannels,
                                   forceAdmin: Boolean
                                 )(implicit ec: ExecutionContext): Future[Option[ScheduleChannelData]] = {
    maybeIsPrivateMemberFor(convo, slackUserId, channels).map { maybeIsPrivateMember =>
      val baseData = ScheduleChannelData(
        convo.id,
        convo.computedName,
        "Slack",
        convo.isBotMember,
        isSelfDm = false,
        isOtherDm = false,
        convo.isPrivateChannel,
        convo.isMpim,
        convo.isArchived,
        convo.isShared
      )
      maybeIsPrivateMember.map { isPrivateMember =>
        if (convo.isVisibleToUserWhere(isPrivateMember, forceAdmin)) {
          Some(baseData.copy(isSelfDm = convo.isIm && isPrivateMember, isOtherDm = convo.isIm && !isPrivateMember))
        } else {
          None
        }
      }.getOrElse(Some(baseData))
    }
  }

  def buildConfigFor(
                      user: User,
                      teamAccess: UserTeamAccess,
                      services: DefaultServices,
                      maybeScheduledId: Option[String],
                      maybeNewSchedule: Option[Boolean],
                      maybeCsrfToken: Option[String],
                      forceAdmin: Boolean
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[ScheduledActionsConfig]] = {
    val dataService = services.dataService
    val cacheService = services.cacheService
    teamAccess.maybeTargetTeam.map { team =>
      for {
        maybeBotProfile <- dataService.slackBotProfiles.allFor(team).map(_.headOption)
        maybeSlackUserId <- if (teamAccess.isAdminAccess) {
          maybeBotProfile.map { botProfile =>
            Future.successful(Some(botProfile.userId))
          }.getOrElse(Future.successful(None))
        } else {
          dataService.linkedAccounts.maybeSlackUserIdFor(user)
        }
        maybeScheduledChannelData <- maybeBotProfile.map { botProfile =>
          val channels = dataService.slackBotProfiles.channelsFor(botProfile)
          channels.getList.flatMap { channelList =>
            Future.sequence(channelList.sortBy(_.sortKey).map { ea =>
              maybeChannelDataFor(ea, maybeSlackUserId.get, channels, forceAdmin)
            }).map(_.flatten).map(Some(_))
          }.recover {
            case e: SlackApiError => None
          }
        }.getOrElse(Future.successful(None))
        scheduledActions <- ScheduledActionData.buildForUserTeamAccess(team, teamAccess, dataService, maybeScheduledChannelData, maybeSlackUserId, forceAdmin)
        behaviorGroups <- dataService.behaviorGroups.allFor(team)
        groupData <- Future.sequence(behaviorGroups.map { group =>
          BehaviorGroupData.maybeFor(group.id, user, None, dataService, cacheService)
        }).map(_.flatten.sorted)
      } yield {
        Some(ScheduledActionsConfig(
          containerId = "scheduling",
          csrfToken = maybeCsrfToken,
          teamId = team.id,
          scheduledActions = scheduledActions,
          channelList = maybeScheduledChannelData,
          behaviorGroups = groupData,
          teamTimeZone = team.maybeTimeZone.map(_.toString),
          teamTimeZoneName = team.maybeTimeZone.map(_.getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
          slackUserId = maybeSlackUserId,
          slackBotUserId = maybeBotProfile.map(_.userId),
          selectedScheduleId = maybeScheduledId,
          newAction = maybeNewSchedule,
          isAdmin = forceAdmin || teamAccess.isAdminAccess
        ))
      }
    }.getOrElse(Future.successful(None))
  }
}
