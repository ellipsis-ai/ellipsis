package models.billing.chargebee

import com.chargebee.models.Plan
import scala.concurrent.Future


trait ChargebeeService {

  def allPlans: Future[Seq[Plan]]

}
