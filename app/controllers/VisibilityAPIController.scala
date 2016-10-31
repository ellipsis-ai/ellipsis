package controllers

import javax.inject.Inject

import json.{InvocationLogEntryData, InvocationLogsByDayData}
import json.Formatting._
import org.joda.time.format.DateTimeFormat
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Action
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VisibilityAPIController @Inject() (
                                 val messagesApi: MessagesApi,
                                 val configuration: Configuration,
                                 val dataService: DataService
                               ) extends EllipsisController {

  case class InvocationCount(
                              date: String,
                              teamName: String,
                              totalCount: Int,
                              uniqueBehaviorCount: Int,
                              uniqueUserCount: Int
                            )

  implicit val invocationCountWrites = Json.writes[InvocationCount]

  private val dateFormatter =  DateTimeFormat.forPattern("EEE, dd MMM yyyy").withLocale(java.util.Locale.ENGLISH)

  def invocationCountsByDay(token: String) = Action.async { implicit request =>
    for {
      maybeTeam <- dataService.teams.findForToken(token)
      isAdmin <- maybeTeam.map { team =>
        dataService.teams.isAdmin(team)
      }.getOrElse(Future.successful(false))
      totalCounts <- dataService.invocationLogEntries.countsByDay
      uniqueBehaviorCounts <- dataService.invocationLogEntries.uniqueInvokedBehaviorCountsByDay
      uniqueUserCounts <- dataService.invocationLogEntries.uniqueInvokingUserCountsByDay
      teamsById <- dataService.teams.allTeams.map(_.groupBy(_.id))
    } yield {
      if (isAdmin) {
        Ok(
          Json.toJson(
            totalCounts.toArray.
              sortBy(_._1.toDate).reverse.
              map { case(date, teamId, totalCount) =>
                val teamName = teamsById.get(teamId).flatMap(_.headOption).map(_.name).getOrElse("<no team>")
                val uniqueBehaviorCount =
                  uniqueBehaviorCounts.
                    find { case(d, tid, bCount) => date == d && teamId == tid }.
                    map(_._3).getOrElse(0)
                val uniqueUserCount =
                  uniqueUserCounts.
                    find { case(d, tid, bCount) => date == d && teamId == tid }.
                    map(_._3).getOrElse(0)
                InvocationCount(date.toString(dateFormatter), teamName, totalCount, uniqueBehaviorCount, uniqueUserCount)
              }
          )
        )
      } else {
        NotFound("")
      }
    }
  }

  def forTeamByDay(token: String, targetTeamName: String) = Action.async { implicit request =>
    for {
      maybeRequestingTeam <- dataService.teams.findForToken(token)
      isAdmin <- maybeRequestingTeam.map { team =>
        dataService.teams.isAdmin(team)
      }.getOrElse(Future.successful(false))
      maybeTargetTeam <- dataService.teams.findByName(targetTeamName)
      entries <- maybeTargetTeam.map { targetTeam =>
        dataService.invocationLogEntries.forTeamByDay(targetTeam)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      if (isAdmin) {
        val data = entries.map { case(date, entries) =>
          InvocationLogsByDayData(
            date.toString(dateFormatter),
            entries.map { ea =>
              InvocationLogEntryData(
                ea.behaviorVersion.behavior.id,
                ea.resultType,
                ea.messageText,
                ea.resultText,
                ea.context,
                ea.maybeUserIdForContext,
                ea.runtimeInMilliseconds,
                ea.createdAt
              )
            }
          )
        }
        Ok(Json.toJson(data))
      } else {
        NotFound("")
      }
    }
  }

}
