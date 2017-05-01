package filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

class HTTPSRedirectFilter @Inject() (override implicit val mat: Materializer) extends Filter {

  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    if (!requestHeader.secure) {
      Future.successful(Results.MovedPermanently("https://" + requestHeader.host + requestHeader.uri))
    } else {
      nextFilter(requestHeader).map(_.withHeaders("Strict-Transport-Security" -> "max-age=31536000; includeSubDomains"))
    }
  }

}
