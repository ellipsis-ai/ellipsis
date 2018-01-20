package models.billing.plan

import com.chargebee.models.Plan
import models.billing.ChargebeeService
import scala.concurrent.Future


trait PlanService extends ChargebeeService {

  def allPlans: Future[Seq[Plan]]

}
