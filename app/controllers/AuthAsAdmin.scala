package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import models.silhouette.EllipsisEnv
import play.api.mvc.{AnyContent, Result}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}


trait AuthAsAdmin extends EllipsisController {

  val dataService: DataService
  implicit val ec: ExecutionContext

  protected def withIsAdminCheck(
                                  fn: () => Future[Result]
                                )(implicit request: SecuredRequest[EllipsisEnv, AnyContent]) = {
    dataService.users.isAdmin(request.identity).flatMap { isAdmin =>
      if (isAdmin) {
        fn()
      } else {
        Future.successful(NotFound(views.html.error.notFound(viewConfig(None), None, None)))
      }
    }
  }

}
