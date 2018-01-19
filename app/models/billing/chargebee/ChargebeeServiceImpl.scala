package models.billing.chargebee


import javax.inject.Inject

import com.chargebee.Environment
import com.chargebee.models.Plan
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

  def allPlans: Future[Seq[Plan]] = {
    Future {
      blocking {
        Plan.list().limit(100).request(chargebeeEnv)
      }
    }.map { result =>
      val buffer = ListBuffer[com.chargebee.models.Plan]()
      for (entry <- result) {
        buffer += entry.plan
      }
      buffer
    }
  }

//  def initializePlans: Future[Seq[Plan]] = {
//    val defaultPlans = Seq[com.chargebee.models.Plan](
//      new Plan(
//        """ {
//            "id": "free-v1",
//            "name": "Free",
//            "price": 0,
//            "period_unit": "month",
//            "charge_model": "flat_fee",
//            "free_quantity": 0,
//            "status": "active",
//            "enabled_in_hosted_pages": true,
//            "enabled_in_portal": true,
//            "updated_at": 1515494920,
//            "resource_version": 1515494920000,
//            "taxable": true,
//            "currency_code": "USD"
//        """.stripMargin)
//    )
//
//
//  }


}



