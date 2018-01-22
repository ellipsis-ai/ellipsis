package models.billing.subscription


import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future, blocking}
import play.api.Configuration
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

  def createFreeSubscription(team: Team, organization: Organization): Future[Subscription] = {
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
      result.subscription()
    }
  }
}
