package filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CspHeaderFilter @Inject() (override implicit val mat: Materializer) extends Filter {
  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    val headers: Seq[(String,String)] = Seq(
      ("X-Frame-Options", "DENY"),
      ("X-Content-Type-Options", "nosniff"),
      ("X-XSS-Protection", "1"),
      ("Content-Security-Policy", "default-src https: 'unsafe-eval' 'unsafe-inline'; object-src 'none'; frame-ancestors 'none'")
    )
    nextFilter(requestHeader).map { result => result.withHeaders(headers: _*) }
  }
}
