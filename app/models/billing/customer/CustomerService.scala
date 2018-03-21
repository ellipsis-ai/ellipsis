package models.billing.customer

import com.chargebee.models.Customer

import scala.concurrent.Future


trait CustomerService {

  def allCustomers(count: Int = 100): Future[Seq[Customer]]

  def delete(customer: Customer): Future[Option[Customer]]

  def delete(customers: Seq[Customer]): Future[Seq[Option[Customer]]]
}
