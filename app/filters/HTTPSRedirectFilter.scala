package filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

class HTTPSRedirectFilter @Inject() (override implicit val mat: Materializer) extends Filter {

  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    requestHeader.headers.get("x-forwarded-proto") match {
      case Some(header) => {
        if ("https" == header) {
          nextFilter(requestHeader).map { result =>
            result.withHeaders(("Strict-Transport-Security", "max-age=31536000"))
          }
        } else {
          Future.successful(Results.Redirect("https://" + requestHeader.host + requestHeader.uri, 301))
        }
      }
      case None => nextFilter(requestHeader)
    }
  }
}
