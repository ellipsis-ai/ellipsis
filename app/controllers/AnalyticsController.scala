package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import models.silhouette.EllipsisEnv
import models.team.{TeamQueries, TeamService}
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Result}
import services.{AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AnalyticsController @Inject() (
                                  val messagesApi: MessagesApi,
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val dataService: DataService,
                                  val lambdaService: AWSLambdaService,
                                  val teamService: TeamService
                                ) extends ReAuthable {

  private def withIsAdminCheck(
                                fn: () => Future[Result]
                              )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]) = {
    dataService.users.isAdmin(request.identity).flatMap { isAdmin =>
      if (isAdmin) {
        fn()
      } else {
        Future.successful(NotFound(views.html.notFound(None, None, None)))
      }
    }
  }

  def index() = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      for {
        allTeam <- teamService.allTeamsSorted
      } yield {
        Ok(views.html.analytics.index(allTeam))
      }
    })
  }

}
