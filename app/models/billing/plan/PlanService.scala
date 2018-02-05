package models.billing.plan


import com.chargebee.models.Plan
import models.billing.ChargebeeService
import scala.concurrent.Future


trait PlanService extends ChargebeeService {

  def allPlans(count: Int = 100): Future[Seq[Plan]]

  def get(planId: String): Future[Option[Plan]]
}
