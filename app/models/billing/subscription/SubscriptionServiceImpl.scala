package models.billing.subscription

import javax.inject.Inject
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, blocking}
import play.api.Configuration
import services.DataService
import com.chargebee.models.Plan
import com.chargebee.models.Subscription


class SubscriptionServiceImpl @Inject()(
                                         val configuration: Configuration,
                                         val dataService: DataService,
                                         implicit val ec: ExecutionContext
                                       ) extends SubscriptionService {

  val freePlanId: String = configuration.get[String]("chargebee.free_plan_id")

  def createFreeSubscription(teamId: String, organizationId: String, customerId: String): Future[Subscription] = {
    Future {
      blocking {
        Subscription.create()
          .planId(freePlanId)
          .param("subscription[teamId]", teamId)
          .customerId(customerId)
          .param("customer[organizationId]", organizationId)
          .request(chargebeeEnv)
      }
    }.map { result =>
      result.subscription()
    }
  }
}
