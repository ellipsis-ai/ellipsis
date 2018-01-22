package data


import javax.inject._
import models.IDs
import com.chargebee.models.Subscription
import services.DataService
import scala.concurrent.{ExecutionContext, Future}


class OrganizationsPopulator @Inject() (
                                     dataService: DataService,
                                     implicit val ec: ExecutionContext
                                   ) {

  def createMissingOrg: Future[Seq[Future[Subscription]]] = {
    dataService.teams.allTeamsWithoutOrg.map { teams =>
      teams.map { team =>
        dataService.organizations.create(team.name).flatMap { org =>
          dataService.teams.setOrganizationIdFor(team, Some(org.id)).flatMap { team =>
            dataService.organizations.setChargebeeCustomerIdFor(org, Some(IDs.next)).flatMap { result =>
              dataService.subscriptions.createFreeSubscription(team, org)
            }
          }
        }
      }
    }
  }

  def run(): Unit = {
    dataService.runNow(createMissingOrg)
  }

  run()
}
