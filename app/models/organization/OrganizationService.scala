package models.organization


import scala.concurrent.Future

trait OrganizationService {

    def allAccounts: Future[Seq[Organization]]

    def count: Future[Int]

    def find(id: String): Future[Option[Organization]]

    def create(name: String): Future[Organization]

    def save(account: Organization): Future[Organization]

}
