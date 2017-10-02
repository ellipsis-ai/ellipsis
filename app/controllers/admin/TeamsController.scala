package controllers.admin

import javax.inject.Inject

import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{AdminController, AuthAsAdmin, ReAuthable, RemoteAssets}
import models.silhouette.EllipsisEnv
import play.api.Configuration
import services.{AWSLambdaService, DataService}

import scala.concurrent.{ExecutionContext, Future}



class TeamsController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val dataService: DataService,
                                  val lambdaService: AWSLambdaService,
                                  val configuration: Configuration,
                                  val assetsProvider: Provider[RemoteAssets],
                                  implicit val ec: ExecutionContext
                                ) extends AuthAsAdmin {

  def list(page: Int, perPage: Int) = silhouette.SecuredAction.async { implicit request =>
    withIsAdminCheck(() => {
      if (page < 0 || perPage < 0) {
        Future {
          BadRequest("page and perPage parameters cannot be less than zero!")
        }
      }
      else {
        for {
          count <- dataService.teams.allCount
          pageData <- getPageData(count, page, perPage)
          teams <- dataService.teams.allTeamsPaged(pageData.current, pageData.size)
        } yield {
          Ok(views.html.admin.teams.list(viewConfig(None), teams, count, pageData.current, pageData.size, pageData.total))
        }
      }

    })
  }


  private case class PageData(current: Int, size: Int, total: Int)

  private def getPageData(count: Int, page: Int, perPage: Int): Future[PageData] = {
    var realPage = page
    var realPerPage = perPage
    var lastPage = (math.ceil(count/realPerPage)).toInt

    // if page is zero and there are less than 50 teams display them all.
    if (page == 0 && count < 50) {
      realPage = 1
      realPerPage = count
      lastPage = 1
    }

    if ( count % realPerPage > 0) lastPage = +1

    Future { new PageData(realPage, realPerPage, lastPage) }
  }

}


