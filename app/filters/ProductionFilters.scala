package filters

import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter
import play.filters.headers.SecurityHeadersFilter

class ProductionFilters @Inject() (
                          https: HTTPSRedirectFilter,
                          csrfFilter: ExcludingCSRFFilter,
                          gzipFilter: GzipFilter,
                          securityHeadersFilter: SecurityHeadersFilter
                          ) extends HttpFilters {

  val filters = Seq(https, csrfFilter, gzipFilter, securityHeadersFilter)
}
