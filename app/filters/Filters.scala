package filters

import javax.inject.Inject
import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter

class Filters @Inject() (
                          https: HTTPSRedirectFilter,
                          csrfFilter: ExcludingCSRFFilter,
                          gzipFilter: GzipFilter,
                          cspHeaderFilter: CspHeaderFilter
                          ) extends HttpFilters {

  val filters = Seq(https, csrfFilter, gzipFilter, cspHeaderFilter)
}
