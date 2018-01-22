package models.organization


import java.time.OffsetDateTime
import drivers.SlickPostgresDriver.api._


class OrganizationsTable(tag: Tag) extends Table[Organization](tag, "organizations") {

  def id = column[String]("id", O.PrimaryKey)
  def name = column[String]("name")
  def maybeChargebeeCustomerId = column[Option[String]]("chargebee_customer_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, name, maybeChargebeeCustomerId, createdAt) <> ((Organization.apply _).tupled, Organization.unapply _)
}

object OrganizationQueries{

  val all = TableQuery[OrganizationsTable]

  def uncompiledFindQueryFor(id: Rep[String]) = {
    all.filter(_.id === id)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

}
