package models.billing.chargebee


import javax.inject.Inject

import com.chargebee.Environment
import com.google.inject.Provider
import play.api.Configuration
import services.DataService

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, blocking}


class ChargebeeServiceImpl @Inject()(
                                      configuration: Configuration,
                                      dataServiceProvider: Provider[DataService],
                                      implicit val ec: ExecutionContext
                                    ) extends ChargebeeService {

  def dataService = dataServiceProvider.get
  val site: String = configuration.get[String]("chargebee.site")
  val apiKey: String = configuration.get[String]("chargebee.api_key")

  val chargebeeEnv = new Environment(site, apiKey)

  def allPlans: Future[Seq[com.chargebee.models.Plan]] = {
    Future {
      blocking {
        com.chargebee.models.Plan.list().limit(5).request(chargebeeEnv)
      }
    }.map { result =>
      val buffer = ListBuffer[com.chargebee.models.Plan]()
      for (entry <- result) {
        buffer += entry.plan
      }
      buffer
    }
  }
}



