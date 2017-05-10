package filters

import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter
import play.filters.headers.SecurityHeadersFilter
import play.filters.cors.CORSFilter

class Filters @Inject() (
                          https: HTTPSRedirectFilter,
                          csrfFilter: ExcludingCSRFFilter,
                          gzipFilter: GzipFilter,
                          securityHeadersFilter: SecurityHeadersFilter,
                          corsFilter: CORSFilter
                          ) extends HttpFilters {

  val filters = Seq(https, csrfFilter, gzipFilter, securityHeadersFilter, corsFilter)
}
