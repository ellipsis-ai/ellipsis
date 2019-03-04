package controllers

import java.time.format.DateTimeFormatter
import java.time.{OffsetDateTime, ZoneOffset}
import javax.inject.Inject

import com.google.inject.Provider
import json.Formatting._
import json.InvocationLogEntryData
import play.api.Configuration
import play.api.libs.json.Json
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

  private def dateFor(year: String, month: String, day: String): OffsetDateTime = {
    OffsetDateTime.of(year.toInt, month.toInt, day.toInt, 0, 0, 0, 0, ZoneOffset.UTC)
  }

  def invocationCountsForDate(token: String, year: String, month: String, day: String) = Action.async { implicit request =>
    val date = dateFor(year, month, day)
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
    val date = dateFor(year, month, day)
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
    val date = dateFor(year, month, day)
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

}
