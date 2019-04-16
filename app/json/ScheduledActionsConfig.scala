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
                                   filterChannelId: Option[String],
                                   newAction: Option[Boolean],
                                   isAdmin: Boolean
                                 )

case class ConversationMemberInfo(isUserMember: Boolean, isBotMember: Boolean)

object ScheduledActionsConfig {

  def maybePrivateMemberInfo(
                                convo: SlackConversation,
                                slackBotProfile: SlackBotProfile,
                                maybeSlackUserProfile: Option[SlackProfile],
                                channels: SlackChannels
                              )(implicit ec: ExecutionContext): Future[Option[ConversationMemberInfo]] = {
    if (convo.isIm || convo.isMpim || convo.isPrivateChannel) {
      channels.getMembersFor(convo.id).map { members =>
        Some(
          ConversationMemberInfo(
            isUserMember = maybeSlackUserProfile.exists(userProfile => members.contains(userProfile.slackUserId)),
            isBotMember = members.contains(slackBotProfile.userId)
          )
        )
      }
    } else {
      Future.successful(None)
    }
  }

  def maybeChannelDataFor(
                           convo: SlackConversation,
                           slackBotProfile: SlackBotProfile,
                           maybeSlackUserProfile: Option[SlackProfile],
                           channels: SlackChannels,
                           isAdmin: Boolean
                         )(implicit ec: ExecutionContext): Future[Option[ScheduleChannelData]] = {
    maybePrivateMemberInfo(convo, slackBotProfile, maybeSlackUserProfile, channels).map { maybePrivateMemberInfo =>
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
      maybePrivateMemberInfo.map { privateMemberInfo =>
        if (convo.isVisibleToUserWhere(privateMemberInfo.isUserMember, isAdmin)) {
          Some(
            baseData.copy(
              isBotMember = privateMemberInfo.isBotMember,
              isSelfDm = convo.isIm && privateMemberInfo.isBotMember && privateMemberInfo.isUserMember,
              isOtherDm = convo.isIm && privateMemberInfo.isBotMember && !privateMemberInfo.isUserMember
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
      tcd.channelList.filter(_.isDm).filter(_.isBotMember)
    }.distinct
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
        maybeChannelDataFor(ea, botProfile, maybeSlackUserProfile, channels, isAdmin)
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
                      maybeFilterChannelId: Option[String],
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
          dataService.behaviorGroups.maybeDataFor(group.id, user)
        }).map(_.flatten.sorted)
      } yield {
        val filterChannelId = maybeFilterChannelId.filter { channelIdWanted =>
          teamChannelsData.exists { data =>
            data.channelList.exists { channel =>
              channel.id == channelIdWanted
            }
          }
        }
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
          filterChannelId = filterChannelId,
          newAction = maybeNewSchedule,
          isAdmin = forceAdmin || teamAccess.isAdminAccess
        ))
      }
    }.getOrElse(Future.successful(None))
  }
}
