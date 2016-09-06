package controllers

import javax.inject.Inject

import models.bots.InvocationLogEntryQueries
import org.joda.time.format.DateTimeFormat
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Action
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

class VisibilityAPIController @Inject() (
                                 val messagesApi: MessagesApi,
                                 val configuration: Configuration,
                                 val dataService: DataService
                               ) extends EllipsisController {

  case class InvocationCount(date: String, count: Int)

  implicit val invocationCountWrites = Json.writes[InvocationCount]

  private val dateFormatter =  DateTimeFormat.forPattern("EEE, dd MMM yyyy").withLocale(java.util.Locale.ENGLISH)

  def invocationCountsByDay(token: String) = Action.async { implicit request =>
    val action = for {
      maybeTeam <- DBIO.from(dataService.teams.findForToken(token))
      isAdmin <- maybeTeam.map { team =>
        DBIO.from(dataService.teams.isAdmin(team))
      }.getOrElse(DBIO.successful(false))
      counts <- InvocationLogEntryQueries.countsByDay
    } yield {
      if (isAdmin) {
        Ok(
          Json.toJson(
            counts.toArray.
              sortBy(_._1.toDate).reverse.
              map { case(date, count) => InvocationCount(date.toString(dateFormatter), count) }
          )
        )
      } else {
        NotFound("")
      }
    }
    dataService.run(action)
  }

}
