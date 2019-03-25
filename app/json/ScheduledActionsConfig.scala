package json

import java.time.format.TextStyle
import java.util.Locale

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.slack.profile.SlackProfile
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
                                   orgChannels: OrgChannelsData,
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

  def maybeUserIsPrivateMember(
                                                   convo: SlackConversation,
                                                   maybeSlackUserProfile: Option[SlackProfile],
                                                   channels: SlackChannels
                                                 )(implicit ec: ExecutionContext): Future[Option[Boolean]] = {
    if (convo.isIm || convo.isMpim || convo.isPrivateChannel) {
      channels.getMembersFor(convo.id).map { members =>
        Some(maybeSlackUserProfile.exists(userProfile => members.contains(userProfile.slackUserId)))
      }
    } else {
      Future.successful(None)
    }
  }

  def maybeChannelDataFor(
                                   convo: SlackConversation,
                                   maybeSlackUserProfile: Option[SlackProfile],
                                   channels: SlackChannels,
                                   isAdmin: Boolean
                                 )(implicit ec: ExecutionContext): Future[Option[ScheduleChannelData]] = {
    maybeUserIsPrivateMember(convo, maybeSlackUserProfile, channels).map { maybeUserIsPrivateMember =>
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
        convo.isExternallyShared,
        convo.isReadOnly,
        convo.isOrgShared
      )
      maybeUserIsPrivateMember.map { isPrivateMember =>
        if (convo.isVisibleToUserWhere(isPrivateMember, isAdmin)) {
          Some(
            baseData.copy(
              isSelfDm = convo.isIm && convo.isBotMember && isPrivateMember,
              isOtherDm = convo.isIm && convo.isBotMember && !isPrivateMember
            )
          )
        } else {
          None
        }
      }.getOrElse(Some(baseData))
    }
  }

  private def orgSharedChannelsDataFor(teamChannelsData: Seq[TeamChannelsData]): Seq[ScheduleChannelData] = {
    teamChannelsData.flatMap { tcd =>
      tcd.channelList.filter(_.isOrgShared).filterNot(_.isOtherDm).filterNot(_.isSelfDm).filterNot(_.isPrivateGroup)
    }.distinct
  }

  private def externallySharedChannelsDataFor(teamChannelsData: Seq[TeamChannelsData]): Seq[ScheduleChannelData] = {
    teamChannelsData.flatMap { tcd =>
      tcd.channelList.filter(_.isExternallyShared).filterNot(_.isOtherDm).filterNot(_.isSelfDm).filterNot(_.isPrivateGroup)
    }.distinct
  }

  private def dmChannelsDataFor(teamChannelsData: Seq[TeamChannelsData]): Seq[ScheduleChannelData] = {
    teamChannelsData.flatMap { tcd =>
      tcd.channelList.filter(_.isSelfDm).filter(_.isBotMember)
    }.slice(0, 1)
  }

  private def mpimChannelsDataFor(teamChannelsData: Seq[TeamChannelsData]): Seq[ScheduleChannelData] = {
    teamChannelsData.flatMap { tcd =>
      tcd.channelList.filter(_.isPrivateGroup)
    }.distinct
  }

  private def maybeTeamChannelsDataFor(
                                        botProfile: SlackBotProfile,
                                        maybeSlackUserProfile: Option[SlackProfile],
                                        isAdmin: Boolean,
                                        services: DefaultServices
                                      )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[TeamChannelsData]] = {
    val channels = services.dataService.slackBotProfiles.channelsFor(botProfile)
    for {
      channelList <- channels.getList
      channelData <- Future.sequence(channelList.sortBy(_.sortKey).map { ea =>
        maybeChannelDataFor(ea, maybeSlackUserProfile, channels, isAdmin)
      }).map(_.flatten)
      maybeTeamInfo <- services.slackApiService.clientFor(botProfile).getTeamInfo
    } yield {
      for {
        teamInfo <- maybeTeamInfo
        teamName <- teamInfo.name
      } yield {
        TeamChannelsData(teamName, channelData)
      }
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
    val isAdminMode = teamAccess.isAdminAccess || forceAdmin
    teamAccess.maybeTargetTeam.map { team =>
      for {
        maybeSlackUserProfile <- if (isAdminMode && !teamAccess.maybeTargetTeam.contains(teamAccess.loggedInTeam)) {
          Future.successful(None)
        } else {
          dataService.users.maybeSlackProfileFor(user)
        }
        botProfiles <- dataService.slackBotProfiles.allFor(team).map { allBotProfiles =>
          if (isAdminMode) {
            allBotProfiles
          } else {
            allBotProfiles.filter { profile =>
              maybeSlackUserProfile.exists(_.teamIds.contains(profile.slackTeamId))
            }
          }
        }
        teamChannelsData <- {
          Future.sequence(botProfiles.map { botProfile =>
            maybeTeamChannelsDataFor(botProfile, maybeSlackUserProfile, isAdminMode, services)
          }).map(_.flatten).recover {
            case e: SlackApiError => Seq()
          }
        }
        scheduledActions <- ScheduledActionData.buildForUserTeamAccess(team, teamAccess, dataService, teamChannelsData, maybeSlackUserProfile.map(_.slackUserId), forceAdmin)
        behaviorGroups <- dataService.behaviorGroups.allFor(team)
        groupData <- Future.sequence(behaviorGroups.map { group =>
          BehaviorGroupData.maybeFor(group.id, user, dataService, cacheService)
        }).map(_.flatten.sorted)
      } yield {
        Some(ScheduledActionsConfig(
          containerId = "scheduling",
          csrfToken = maybeCsrfToken,
          teamId = team.id,
          scheduledActions = scheduledActions,
          orgChannels = OrgChannelsData(
            dmChannels = dmChannelsDataFor(teamChannelsData),
            mpimChannels = mpimChannelsDataFor(teamChannelsData),
            orgSharedChannels = orgSharedChannelsDataFor(teamChannelsData),
            externallySharedChannels = externallySharedChannelsDataFor(teamChannelsData),
            teamChannels = teamChannelsData.map(_.copyWithoutCommonChannels)
          ),
          behaviorGroups = groupData,
          teamTimeZone = team.maybeTimeZone.map(_.toString),
          teamTimeZoneName = team.maybeTimeZone.map(_.getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
          slackUserId = maybeSlackUserProfile.map(_.slackUserId),
          slackBotUserId = botProfiles.headOption.map(_.userId),
          selectedScheduleId = maybeScheduledId,
          newAction = maybeNewSchedule,
          isAdmin = forceAdmin || teamAccess.isAdminAccess
        ))
      }
    }.getOrElse(Future.successful(None))
  }
}
