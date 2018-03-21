package models.billing.customer

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future, blocking}
import play.api.{Configuration, Logger}
import services.DataService
import com.chargebee.models.{Customer, Subscription}
import com.google.inject.Provider
import models.billing.ChargebeeService
import models.organization.Organization

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer


class CustomerServiceImpl @Inject()(
                                         val dataServiceProvider: Provider[DataService],
                                         val configuration: Configuration,
                                         implicit val ec: ExecutionContext
                                       ) extends CustomerService with ChargebeeService {
  def dataService = dataServiceProvider.get


  def allCustomers(count: Int = 100): Future[Seq[Customer]] = {
      Future {
        blocking {
          Customer.list().limit(count).request(chargebeeEnv)
        }
      }.map { result =>
        val buffer = ListBuffer[Customer]()
        for (entry <- result) {
          buffer += entry.customer
        }
        buffer
      }
  }

  def delete(customer: Customer): Future[Option[Customer]] = {
    Future {
      blocking {
        Some(Customer.delete(customer.id).request(chargebeeEnv).customer())
      }
    }.recover {
      case e: Throwable => {
        Logger.error(s"Error while deleting Customer with id ${customer.id}", e)
        None
      }
    }
  }

   def delete(customers: Seq[Customer]): Future[Seq[Option[Customer]]] = Future.sequence(customers.map(delete(_)))

}

