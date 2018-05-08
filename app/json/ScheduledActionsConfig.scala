package json

import java.time.format.TextStyle
import java.util.Locale

import akka.actor.ActorSystem
import models.accounts.user.{User, UserTeamAccess}
import services.DefaultServices

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
                                   newAction: Option[Boolean]
                                 )

object ScheduledActionsConfig {
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
        maybeChannelList <- maybeBotProfile.map { botProfile =>
          val channels = dataService.slackBotProfiles.channelsFor(botProfile, cacheService)
          if (forceAdmin) {
            channels.getList.map(Some(_))
          } else {
            channels.getListForUser(maybeSlackUserId).map(Some(_)).recover {
              case e: slack.api.ApiError => None
            }
          }
        }.getOrElse(Future.successful(None))
        scheduledActions <- ScheduledActionData.buildForUserTeamAccess(team, teamAccess, dataService, maybeChannelList, maybeSlackUserId, forceAdmin)
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
          channelList = maybeChannelList.map(ScheduleChannelData.fromChannelLikeList),
          behaviorGroups = groupData,
          teamTimeZone = team.maybeTimeZone.map(_.toString),
          teamTimeZoneName = team.maybeTimeZone.map(_.getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
          slackUserId = maybeSlackUserId,
          slackBotUserId = maybeBotProfile.map(_.userId),
          selectedScheduleId = maybeScheduledId,
          newAction = maybeNewSchedule
        ))
      }
    }.getOrElse(Future.successful(None))
  }
}
