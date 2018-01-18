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

  def allPlans: Future[Seq[models.billing.chargebee.Plan]] = {
    Environment.configure("{site}","{site_api_key}")

    ListResult result = com.chargebee.models.Plan.list().limit(5).request()
    val iterator = result.iterator().asScala
    val listOfPlans: Seq[models.billing.chargebee.Plan] = Seq[models.billing.chargebee.Plan]
    iterator.foreach { entry =>
      val entity = entry.plan()
      val json = entity.toJson.replace("\"id\"", "\"_id\"")
      models.billing.chargebee.Plan
    }
  }
}



