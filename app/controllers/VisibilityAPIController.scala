package controllers

import java.time.format.DateTimeFormatter
import java.time.{OffsetDateTime, ZoneOffset}

import javax.inject.Inject
import com.google.inject.Provider
import json.Formatting._
import json.InvocationLogEntryData
import models.behaviors.events.EventType
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request, Result}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class VisibilityAPIController @Inject() (
                                 val configuration: Configuration,
                                 val dataService: DataService,
                                 val assetsProvider: Provider[RemoteAssets],
                                 implicit val ec: ExecutionContext
                               ) extends EllipsisController {

  case class InvocationCount(
                              date: String,
                              teamName: String,
                              totalCount: Int,
                              uniqueBehaviorCount: Int,
                              uniqueUserCount: Int
                            )

  implicit val invocationCountWrites = Json.writes[InvocationCount]

  private val dateFormatter =  DateTimeFormatter.ofPattern("EEE, dd MMM yyyy").withLocale(java.util.Locale.ENGLISH)

  private def startOfDayFor(year: String, month: String, day: String): OffsetDateTime = {
    OffsetDateTime.of(year.toInt, month.toInt, day.toInt, 0, 0, 0, 0, ZoneOffset.UTC)
  }

  def invocationCountsForDate(token: String, year: String, month: String, day: String) = Action.async { implicit request =>
    val date = startOfDayFor(year, month, day)
    for {
      maybeTeam <- dataService.teams.findForInvocationToken(token)
      isAdmin <- maybeTeam.map { team =>
        dataService.teams.isAdmin(team)
      }.getOrElse(Future.successful(false))
      totalCounts <- dataService.invocationLogEntries.countsForDate(date)
      uniqueBehaviorCounts <- dataService.invocationLogEntries.uniqueInvokedBehaviorCountsForDate(date)
      uniqueUserCounts <- dataService.invocationLogEntries.uniqueInvokingUserCountsForDate(date)
      teamsById <- dataService.teams.allTeams.map(_.groupBy(_.id))
    } yield {
      if (isAdmin) {
        Ok(
          Json.toJson(
            totalCounts.
              map { case(teamId, totalCount) =>
                val teamName = teamsById.get(teamId).flatMap(_.headOption).map(_.name).getOrElse("<no team>")
                val uniqueBehaviorCount =
                  uniqueBehaviorCounts.
                    find { case(tid, bCount) => teamId == tid }.
                    map(_._2).getOrElse(0)
                val uniqueUserCount =
                  uniqueUserCounts.
                    find { case(tid, bCount) => teamId == tid }.
                    map(_._2).getOrElse(0)
                InvocationCount(dateFormatter.format(date), teamName, totalCount, uniqueBehaviorCount, uniqueUserCount)
              }
          )
        )
      } else {
        NotFound("")
      }
    }
  }

  def forTeamForDate(token: String, targetTeamName: String, year: String, month: String, day: String) = Action.async { implicit request =>
    val date = startOfDayFor(year, month, day)
    for {
      maybeRequestingTeam <- dataService.teams.findForInvocationToken(token)
      isAdmin <- maybeRequestingTeam.map { team =>
        dataService.teams.isAdmin(team)
      }.getOrElse(Future.successful(false))
      maybeTargetTeam <- dataService.teams.findByName(targetTeamName)
      entries <- maybeTargetTeam.map { targetTeam =>
        dataService.invocationLogEntries.forTeamForDate(targetTeam, date)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      if (isAdmin) {
        val data = entries.map { ea =>
          InvocationLogEntryData(
            ea.behaviorVersion.behavior.id,
            ea.resultType,
            ea.messageText,
            ea.resultText,
            ea.context,
            ea.maybeChannel,
            ea.maybeUserIdForContext,
            ea.maybeOriginalEventType.map(_.toString),
            ea.runtimeInMilliseconds,
            ea.createdAt
          )
        }
        Ok(Json.toJson(data))
      } else {
        NotFound("")
      }
    }
  }

  def forTeamSinceDate(
                         token: String,
                         targetTeamId: String,
                         year: String,
                         month: String,
                         day: String
                       ) = Action.async { implicit request =>
    val date = startOfDayFor(year, month, day)
    for {
      maybeRequestingTeam <- dataService.teams.findForInvocationToken(token)
      isAdmin <- maybeRequestingTeam.map { team =>
        dataService.teams.isAdmin(team)
      }.getOrElse(Future.successful(false))
      maybeTargetTeam <- dataService.teams.find(targetTeamId)
      entries <- maybeTargetTeam.map { targetTeam =>
        dataService.invocationLogEntries.forTeamSinceDate(targetTeam, date)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      if (isAdmin) {
        val data = entries.map { ea =>
          InvocationLogEntryData(
            ea.behaviorVersion.behavior.id,
            ea.resultType,
            ea.messageText,
            ea.resultText,
            ea.context,
            ea.maybeChannel,
            ea.maybeUserIdForContext,
            ea.maybeOriginalEventType.map(_.toString),
            ea.runtimeInMilliseconds,
            ea.createdAt
          )
        }
        Ok(Json.toJson(data))
      } else {
        NotFound("")
      }
    }
  }

  case class PerBehaviorActiveWorkflowStat(
                                            actionName: String,
                                            skillName: String,
                                            start: OffsetDateTime,
                                            end: OffsetDateTime,
                                            invocationCount: Long,
                                            involvedUserCount: Long
                                          )

  implicit val perBehaviorActiveWorkflowStatWrites = Json.writes[PerBehaviorActiveWorkflowStat]

  case class PerSkillActiveWorkflowStats(
                                          skillName: String,
                                          invocationCount: Long,
                                          involvedUserCount: Long,
                                          workflows: Seq[PerBehaviorActiveWorkflowStat]
                                        )

  implicit val perSkillActiveWorkflowStatWrites = Json.writes[PerSkillActiveWorkflowStats]

  case class ActiveWorkflowStats(
                                  activeWorkflowCount: Long,
                                  activeSkillCount: Long,
                                  involvedUserCount: Long,
                                  skills: Seq[PerSkillActiveWorkflowStats]
                               )

  implicit val aggregateActiveWorkflowStatWrites = Json.writes[ActiveWorkflowStats]

  val workflowEventTypes: Seq[EventType] = Seq(EventType.scheduled, EventType.chat)

  def activeWorkflowsSinceDate(
                                token: String,
                                targetTeamName: String,
                                year: String,
                                month: String,
                                day: String
                              ) = Action.async { implicit request =>
    val start = startOfDayFor(year, month, day)
    val end = OffsetDateTime.now
    activeWorkflowsBetween(token, targetTeamName, start, end)
  }

  def activeWorkflowsFromDateToDate(
                                     token: String,
                                     targetTeamName: String,
                                     fromYear: String,
                                     fromMonth: String,
                                     fromDay: String,
                                     toYear: String,
                                     toMonth: String,
                                     toDay: String
                                   ) = Action.async { implicit request =>
    val start = startOfDayFor(fromYear, fromMonth, fromDay)
    val inclusiveEnd = startOfDayFor(toYear, toMonth, toDay).plusDays(1)
    activeWorkflowsBetween(token, targetTeamName, start, inclusiveEnd)
  }

  def activeWorkflowsBetween(
                              token: String,
                              targetTeamName: String,
                              start: OffsetDateTime,
                              end: OffsetDateTime
                            )(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      maybeRequestingTeam <- dataService.teams.findForInvocationToken(token)
      isAdmin <- maybeRequestingTeam.map { team =>
        dataService.teams.isAdmin(team)
      }.getOrElse(Future.successful(false))
      maybeTargetTeam <- dataService.teams.findByName(targetTeamName)
      entries <- maybeTargetTeam.map { targetTeam =>
        dataService.invocationLogEntries.forTeamSinceDate(targetTeam, start).map { e =>
          e.filter { ea =>
            ea.maybeEventType.exists(et => workflowEventTypes.contains(et))
          }
        }
      }.getOrElse(Future.successful(Seq()))
      involvements <- maybeTargetTeam.map { targetTeam =>
        dataService.behaviorVersionUserInvolvements.findAllForTeamBetween(targetTeam, start, end)
      }.getOrElse(Future.successful(Seq()))
      behaviorsToCount <- Future.successful(entries.map(_.behaviorVersion.behavior).distinct.filterNot(_.isDataType))
      mostRecentVersionsForBehaviors <- Future.sequence(behaviorsToCount.map { ea =>
        for {
          maybeCurrentGroupVersion <- ea.maybeGroup.map { group =>
            dataService.behaviorGroupVersions.maybeCurrentFor(group)
          }.getOrElse(Future.successful(None))
          maybeCurrentBehaviorVersion <- maybeCurrentGroupVersion.map { groupVersion =>
            dataService.behaviorVersions.findFor(ea, groupVersion)
          }.getOrElse(Future.successful(None))
        } yield maybeCurrentBehaviorVersion.map { bv =>
          (ea, bv)
        }
      }).map(_.flatten.toMap)
    } yield {
      if (isAdmin) {
        val involvementsToCount = involvements.filter(ea => behaviorsToCount.contains(ea.behaviorVersion.behavior))
        val perSkillData = involvementsToCount.groupBy(_.behaviorVersion.group).flatMap { case (behaviorGroup, involvementsForGroup) =>
          val uniqueUserCount = involvementsForGroup.map(_.user).distinct.length
          val invocationCount = involvementsForGroup.groupBy(_.createdAt).size
          val perActionData = involvementsForGroup.groupBy(_.behaviorVersion.behavior).map { case(behavior, involvementsGroup) =>
            val uniqueUserCount = involvementsGroup.map(_.user).distinct.length
            val invocationCount = involvementsGroup.groupBy(_.createdAt).size
            val maybeBehaviorVersion = mostRecentVersionsForBehaviors.get(behavior)
            val behaviorName = maybeBehaviorVersion.flatMap(_.maybeName).getOrElse(behavior.id)
            val skillName = maybeBehaviorVersion.map(_.groupVersion.name).getOrElse("Unnamed skill")
            (behaviorName, skillName, uniqueUserCount, invocationCount)
          }.map { case(actionName, skillName, uniqueUserCount, invocationCount) =>
            PerBehaviorActiveWorkflowStat(
              actionName,
              skillName,
              start,
              end,
              invocationCount,
              uniqueUserCount
            )
          }.toSeq
          perActionData.headOption.map { actionData =>
            PerSkillActiveWorkflowStats(
              actionData.skillName,
              invocationCount,
              uniqueUserCount,
              perActionData
            )
          }
        }.toSeq

        val totalActiveSkills = perSkillData.size
        val totalActiveWorkflows = perSkillData.map(_.workflows.size).sum
        val totalInvolvedUsers = involvementsToCount.map(_.user).distinct.size
        val overallStats = ActiveWorkflowStats(totalActiveWorkflows, totalActiveSkills, totalInvolvedUsers, perSkillData)
        Ok(Json.toJson(overallStats))
      } else {
        NotFound("")
      }
    }
  }


}
