package models.billing.subscription

import java.time.OffsetDateTime

import drivers.SlickPostgresDriver.api._

class SubscriptionsTable(tag: Tag) extends Table[Subscription](tag, "billing_subscriptions") {

  def id = column[String]("id")
  def chargebeePlanId = column[String]("chargebeePlanId")
  def accountId = column[String]("accountId")
  def teamId = column[String]("teamId")
  def seatCount = column[Int]("seatCount")
  def status = column[String]("status")
  def status_updated_at = column[OffsetDateTime]("created_at")
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, chargebeePlanId, accountId, teamId, seatCount, status, status_updated_at, createdAt) <> ((Subscription.apply _).tupled, Subscription.unapply _)
}

object SubscriptionQueries {

  val all = TableQuery[SubscriptionsTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

}
