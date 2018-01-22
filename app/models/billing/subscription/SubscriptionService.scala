package models.billing.subscription


import scala.concurrent.Future
import com.chargebee.models.Subscription
import models.organization.Organization
import models.team.Team


trait SubscriptionService {

  def createFreeSubscription(team: Team, organization: Organization): Future[Subscription]

}
