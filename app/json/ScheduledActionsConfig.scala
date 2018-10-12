package json

import java.time.format.TextStyle
import java.util.Locale

import akka.actor.ActorSystem
import models.accounts.slack.botprofile.SlackBotProfile
import models.accounts.user.{User, UserTeamAccess}
import services.DefaultServices
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

  private def maybeIsPrivateMemberAlongWithBotFor(convo: SlackConversation, slackUserId: String, channels: SlackChannels)(implicit ec: ExecutionContext): Future[Option[Boolean]] = {
    if (convo.isIm || convo.isMpim || convo.isPrivateChannel) {
      channels.getMembersFor(convo.id).map { members =>
        Some(members.contains(slackUserId) && members.contains(channels.client.profile.userId))
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
    maybeIsPrivateMemberAlongWithBotFor(convo, slackUserId, channels).map { maybeIsPrivateMemberAlongWithBot =>
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
      maybeIsPrivateMemberAlongWithBot.map { isPrivateMemberAlongWithBot =>
        if (convo.isVisibleToUserWhere(isPrivateMemberAlongWithBot, forceAdmin)) {
          Some(
            baseData.copy(
              isSelfDm = convo.isIm && isPrivateMemberAlongWithBot,
              isOtherDm = convo.isIm && !isPrivateMemberAlongWithBot,
              isBotMember = isPrivateMemberAlongWithBot
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
                                        slackUserId: String,
                                        forceAdmin: Boolean,
                                        services: DefaultServices
                                      )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[Option[TeamChannelsData]] = {
    val channels = services.dataService.slackBotProfiles.channelsFor(botProfile)
    for {
      channelList <- channels.getList
      channelData <- Future.sequence(channelList.sortBy(_.sortKey).map { ea =>
        maybeChannelDataFor(ea, slackUserId, channels, forceAdmin)
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
    teamAccess.maybeTargetTeam.map { team =>
      for {
        botProfiles <- dataService.slackBotProfiles.allFor(team)
        maybeSlackUserId <- if (teamAccess.isAdminAccess) {
          botProfiles.headOption.map { botProfile =>
            Future.successful(Some(botProfile.userId))
          }.getOrElse(Future.successful(None))
        } else {
          dataService.linkedAccounts.maybeSlackUserIdFor(user)
        }
        teamChannelsData <- Future.sequence(botProfiles.map { botProfile =>
          maybeTeamChannelsDataFor(botProfile, maybeSlackUserId.get, forceAdmin, services)
        }).map(_.flatten)
        scheduledActions <- ScheduledActionData.buildForUserTeamAccess(team, teamAccess, dataService, teamChannelsData, maybeSlackUserId, forceAdmin)
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
          slackUserId = maybeSlackUserId,
          slackBotUserId = botProfiles.headOption.map(_.userId),
          selectedScheduleId = maybeScheduledId,
          newAction = maybeNewSchedule,
          isAdmin = forceAdmin || teamAccess.isAdminAccess
        ))
      }
    }.getOrElse(Future.successful(None))
  }
}
