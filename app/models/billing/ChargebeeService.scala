package models.billing


import com.chargebee.Environment
import play.api.Configuration
import scala.concurrent.ExecutionContext


trait ChargebeeService {

  val configuration: Configuration
  implicit val ec: ExecutionContext

  val site: String = configuration.get[String]("billing.chargebee.site")
  val apiKey: String = configuration.get[String]("billing.chargebee.api_key")
  val chargebeeEnv = new Environment(site, apiKey)
  val freePlanId: String = configuration.get[String]("billing.chargebee.free_plan_id")

}
