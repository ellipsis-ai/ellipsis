package filters

import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CspHeaderFilter extends Filter {
  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    val headers: Seq[(String,String)] = Seq(
      ("X-Frame-Options", "DENY"),
      ("Content-Security-Policy", "frame-ancestors 'none'")
    )
    nextFilter(requestHeader).map { result => result.withHeaders(headers: _*) }
  }
}
