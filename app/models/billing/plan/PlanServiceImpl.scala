package models.billing.plan


import javax.inject.Inject

import com.chargebee.models.{Plan, Subscription}
import com.google.inject.Provider
import play.api.Configuration
import services.DataService

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, blocking}


class PlanServiceImpl @Inject()(
                                 val configuration: Configuration,
                                 val dataServiceProvider: Provider[DataService],
                                 implicit val ec: ExecutionContext
                               ) extends PlanService {

  def dataService = dataServiceProvider.get

  def allPlans(count: Int = 100): Future[Seq[Plan]] = {
      Future {
        blocking {
          Plan.list().limit(count).request(chargebeeEnv)
        }
      }.map { result =>
        val buffer = ListBuffer[com.chargebee.models.Plan]()
        for (entry <- result) {
          buffer += entry.plan
        }
        buffer
      }
  }

  def get(planId: String): Future[Option[Plan]] = {
    Future {
      blocking{
        Some(Plan.retrieve(planId).request(chargebeeEnv).plan())
      }
    }
  }

}
