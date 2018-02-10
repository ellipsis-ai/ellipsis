package models.billing.subscription


import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future, blocking}
import play.api.{Configuration, Logger}
import services.DataService
import com.chargebee.models.Subscription
import com.chargebee.models.enums.AutoCollection
import com.google.inject.Provider
import models.billing.ChargebeeService
import models.organization.Organization

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer


case class MissingChargebeeCustomerId(message: String) extends Exception

class SubscriptionServiceImpl @Inject()(
                                         val dataServiceProvider: Provider[DataService],
                                         val configuration: Configuration,
                                         implicit val ec: ExecutionContext
                                       ) extends SubscriptionService with ChargebeeService {
  def dataService = dataServiceProvider.get

  def find(subscriptionId: String): Future[Option[Subscription]] = {
    Future {
      blocking{
        Some(Subscription.retrieve(subscriptionId).request(chargebeeEnv).subscription())
      }
    }
  }

  def createFreeSubscription(organization: Organization): Future[Option[Subscription]] = {
    Future {
      blocking {
        organization.maybeChargebeeCustomerId match {
          case None => throw new MissingChargebeeCustomerId("Organization must have a valid Chargebee Customer Id set!")
          case Some(chargebeeCustomerId) => {
            Some(Subscription.create()
              .planId(freePlanId)
              .autoCollection(AutoCollection.OFF)
              .param("cf_organization_id", organization.id)
              .param("cf_organization_name", organization.name)
              .customerId(chargebeeCustomerId)
              .param("customer[cf_organization_id]", organization.id)
              .param("customer[cf_organization_name]", organization.name)
              .request(chargebeeEnv).subscription())
          }
        }
      }
    }.recover {
      case e: Throwable => {
        Logger.error(s"Error while creating a free subscription for Org ${organization.name}", e)
        None
      }
    }
  }

  def allSubscriptions(count: Int = 100): Future[Seq[Subscription]] = {
      Future {
        blocking {
          Subscription.list().limit(count).request(chargebeeEnv)
        }
      }.map { result =>
        val buffer = ListBuffer[com.chargebee.models.Subscription]()
        for (entry <- result) {
          buffer += entry.subscription
        }
        buffer
      }
  }

  def delete(subscription: Subscription): Future[Option[Subscription]] = {
    Future {
      blocking {
        Subscription.delete(subscription.id).request(chargebeeEnv).subscription()
      }
    }.map(Some(_)).recover {
      case e: Throwable => {
        Logger.error(s"Error while deleting Subscription with id ${subscription.id}", e)
        None
      }
    }
  }

   def delete(subs: Seq[Subscription]): Future[Seq[Option[Subscription]]] = Future.sequence(subs.map(delete(_)))

}

