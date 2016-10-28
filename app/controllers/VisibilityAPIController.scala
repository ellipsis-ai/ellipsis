package controllers

import javax.inject.Inject

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
                    filter { case(d, tid, bCount) => date == d && teamId == tid }.
                    headOption.map(_._3).getOrElse(0)
                val uniqueUserCount =
                  uniqueUserCounts.
                    filter { case(d, tid, bCount) => date == d && teamId == tid }.
                    headOption.map(_._3).getOrElse(0)
                InvocationCount(date.toString(dateFormatter), teamName, totalCount, uniqueBehaviorCount, uniqueUserCount)
              }
          )
        )
      } else {
        NotFound("")
      }
    }
  }

}
