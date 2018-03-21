package models.billing


import com.chargebee.Environment
import com.chargebee.models.Plan
import play.api.Configuration

import scala.concurrent.ExecutionContext


trait ChargebeeService {

  val configuration: Configuration
  implicit val ec: ExecutionContext

  val site: String = configuration.get[String]("billing.chargebee.site")
  val apiKey: String = configuration.get[String]("billing.chargebee.api_key")
  val chargebeeEnv = new Environment(site, apiKey)
  val freePlanId: String = configuration.get[String]("billing.chargebee.free_plan_id")
  val starterPlanId: String = configuration.get[String]("billing.chargebee.starter_plan_id")
  val businessPlanId: String = configuration.get[String]("billing.chargebee.business_plan_id")

  def addonIdFor(plan: Plan): String = {
    s"active-user-${plan.id}"
  }
}
