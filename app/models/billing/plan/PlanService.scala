package models.billing.plan


import com.chargebee.models.Plan
import models.billing.ChargebeeService
import scala.concurrent.Future


trait PlanService extends ChargebeeService {

  def allPlans(count: Int = 100): Future[Seq[Plan]]

  def find(id: String): Future[Option[Plan]]

  def create(data: PlanData, doNotLogError: Boolean = false): Future[Option[Plan]]

  def createStandardPlans(doNotLogError: Boolean = false): Future[Seq[Option[Plan]]]

  def delete(plan: Plan): Future[Option[Plan]]

  def delete(plans: Seq[Plan]): Future[Seq[Option[Plan]]]
}
