package filters

import javax.inject.Inject

import play.api.mvc.{EssentialFilter, EssentialAction}
import play.filters.csrf.CSRFFilter

class ExcludingCSRFFilter @Inject() (csrfFilter: CSRFFilter) extends EssentialFilter {
  override def apply(nextFilter: EssentialAction) = new EssentialAction {

    import play.api.mvc._

    override def apply(rh: RequestHeader) = {
      if (rh.tags.getOrElse("ROUTE_COMMENTS", "").contains("NOCSRF")) {
        nextFilter(rh)
      } else {
        val chainedFilter = csrfFilter.apply(nextFilter)
        chainedFilter(rh)
      }
    }
  }
}
