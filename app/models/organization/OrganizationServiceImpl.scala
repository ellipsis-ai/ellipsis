package models.organization


import java.time.OffsetDateTime
import javax.inject.Inject

import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import models.IDs
import services.DataService

import scala.concurrent.{ExecutionContext, Future}


class OrganizationServiceImpl @Inject()(
                                    dataServiceProvider: Provider[DataService],
                                    implicit val ec: ExecutionContext
                                  ) extends OrganizationService {

  def dataService = dataServiceProvider.get

  import OrganizationQueries._

  def allOrganizations: Future[Seq[Organization]] = {
    dataService.run(all.result)
  }

  def allOrgsWithEmptyChargebeeId: Future[Seq[Organization]] = {
    dataService.run(withEmptyChargebeeId.result)
  }

  def count: Future[Int] = {
    dataService.run(all.length.result)
  }

  def find(id: String): Future[Option[Organization]] = {
    dataService.run(findQueryFor(id).result.map(_.headOption))
  }

  def create(name: String): Future[Organization] = save(
    Organization(
      IDs.next,
      name,
      None,
      OffsetDateTime.now()
    )
  )

  def createAction(name: String): DBIO[Organization] = saveAction(
    Organization(
      IDs.next,
      name,
      None,
      OffsetDateTime.now()
    )
  )

  def save(organization: Organization): Future[Organization] = {
    dataService.run(saveAction(organization))
  }

  def setChargebeeCustomerIdFor(organization: Organization, chargebeeCustomerId: Option[String]): Future[Organization] = {
    save(organization.copy(maybeChargebeeCustomerId = chargebeeCustomerId))
  }

  def setChargebeeCustomerIdFor(organization: Organization, chargebeeCustomerId: String): Future[Organization] = {
    save(organization.copy(maybeChargebeeCustomerId = Some(chargebeeCustomerId)))
  }


  private def saveAction(organization: Organization): DBIO[Organization] = {
    findQueryFor(organization.id).result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.id === organization.id).update(organization)
      }.getOrElse {
        all += organization
      }.map { _ => organization }
    }
  }

}
