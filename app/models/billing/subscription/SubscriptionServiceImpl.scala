package models.billing.subscription


import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future, blocking}
import play.api.{Configuration, Logger}
import services.DataService
import com.chargebee.models.Subscription
import com.google.inject.Provider
import models.billing.ChargebeeService
import models.organization.Organization
import models.team.Team


class SubscriptionServiceImpl @Inject()(
                                         val dataServiceProvider: Provider[DataService],
                                         val configuration: Configuration,
                                         implicit val ec: ExecutionContext
                                       ) extends SubscriptionService with ChargebeeService {
  def dataService = dataServiceProvider.get

  def createFreeSubscription(team: Team, organization: Organization): Future[Option[Subscription]] = {
    Future {
      blocking {
        Subscription.create()
          .planId(freePlanId)
          .param("cf_team_id", team.id)
          .param("subscription[cf_team_name]", team.name)
          .customerId(organization.maybeChargebeeCustomerId.get)
          .param("customer[cf_organization_id]", organization.id)
          .param("customer[cf_organization_name]", organization.name)
          .request(chargebeeEnv)
      }
    }.map { result =>
      Some(result.subscription())
    }.recover {
      // If the Chargebee API fails we log a message an re-throw. This is cause a 500 and Sentry will
      // let us know. If the Chargebee API is not very reliable then we can change this.
      case e: Throwable => {
        Logger.error(s"Error while creating a free subscription for team ${team.name}", e)
        throw e
      }
    }
  }

}

