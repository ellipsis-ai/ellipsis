package models.organization

import drivers.SlickPostgresDriver.api._
import models.team.Team

import scala.concurrent.Future


trait OrganizationService {

    def allOrganizations: Future[Seq[Organization]]

    def allOrgsWithEmptyChargebeeId: Future[Seq[Organization]]

    def count: Future[Int]

    def find(id: String): Future[Option[Organization]]

    def create(name: String): Future[Organization]

    def create(name: String, chargebeeCustomerId: String): Future[Organization]

    def createAction(name: String): DBIO[Organization]

    def save(organization: Organization): Future[Organization]

    def setChargebeeCustomerIdFor(organization: Organization, customerId: Option[String]): Future[Organization]

}
