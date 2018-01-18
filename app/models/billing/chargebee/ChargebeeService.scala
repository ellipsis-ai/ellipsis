package models.billing.chargebee

import scala.concurrent.Future


trait ChargebeeService {

  def allPlans: Future[Seq[com.chargebee.models.Plan]]
}
