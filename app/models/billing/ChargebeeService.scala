package models.billing

import com.chargebee.Environment
import play.api.Configuration
import services.DataService

import scala.concurrent.ExecutionContext

trait ChargebeeService {

  val configuration: Configuration
  val dataService: DataService
  implicit val ec: ExecutionContext

  val site: String = configuration.get[String]("chargebee.site")
  val apiKey: String = configuration.get[String]("chargebee.api_key")
  val chargebeeEnv = new Environment(site, apiKey)

}
