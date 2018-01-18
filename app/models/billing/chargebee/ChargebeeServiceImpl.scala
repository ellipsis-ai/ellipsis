package models.billing.chargebee


import javax.inject.Inject

import com.google.inject.Provider
import services.DataService
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}
import com.chargebee.{Environment, ListResult}
import com.chargebee.models._
import com.chargebee.models.enums._;


class ChargebeeServiceImpl @Inject()(
                                      configuration: Configuration,
                                      dataServiceProvider: Provider[DataService],
                                      implicit val ec: ExecutionContext
                                    ) extends ChargebeeService {

  def dataService = dataServiceProvider.get
  val site: String = config.get[String]("chargebee.site")
  val apiKey: String = config.get[String]("chargebee.api_key")

  def allPlans: Future[Seq[models.billing.chargebee.Plan]] = {
    val listOfPlans: Seq[models.billing.chargebee.Plan] = Seq[models.billing.chargebee.Plan]()

    Environment.configure(site,apiKey)
    ListResult result = com.chargebee.models.Plan.list().limit(100).request()
    val iterator = result.iterator().asScala

    iterator.foreach { entry =>
      val entity = entry.plan()
      val json = entity.toJson.replace("\"id\"", "\"_id\"")
      models.billing.chargebee.Plan
    }
  }
}



