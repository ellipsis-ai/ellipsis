package filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class HTTPSRedirectFilter @Inject() (
                                      override implicit val mat: Materializer,
                                      implicit val ec: ExecutionContext
                                    ) extends Filter {

  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    requestHeader.headers.get("x-forwarded-proto") match {
      case Some(header) => {
        if ("https" == header) {
          nextFilter(requestHeader).map { result =>
            result.withHeaders(("Strict-Transport-Security", "max-age=31536000; includeSubDomains"))
          }
        } else {
          Future.successful(Results.MovedPermanently("https://" + requestHeader.host + requestHeader.uri))
        }
      }
      case None => nextFilter(requestHeader)
    }
  }
}
