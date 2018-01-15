package models.billing.account

import java.time.OffsetDateTime

import drivers.SlickPostgresDriver.api._

class AccountsTable(tag: Tag) extends Table[Account](tag, "billing_accounts") {

  def id = column[String]("id", O.PrimaryKey)
  def chargebeeId = column[String]("chargebee_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, chargebeeId, createdAt) <> ((Account.apply _).tupled, Account.unapply _)
}

object AccountQueries {

  val all = TableQuery[AccountsTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

}
