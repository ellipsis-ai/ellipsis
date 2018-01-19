package models.billing.account

import java.time.OffsetDateTime

import drivers.SlickPostgresDriver.api._

class CustomersTable(tag: Tag) extends Table[Customer](tag, "billing_accounts") {

  def id = column[String]("id", O.PrimaryKey)
  def chargebeeId = column[String]("chargebee_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, chargebeeId, createdAt) <> ((Customer.apply _).tupled, Customer.unapply _)
}

object CustomerQueries {

  val all = TableQuery[CustomersTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

}
