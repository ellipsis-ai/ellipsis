package data

import javax.inject._

import models.organization.OrganizationService
import models.team.{Team, TeamService}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class OrganizationsPopulator @Inject() (
                                     dataService: DataService,
                                     implicit val ec: ExecutionContext
                                   ) {

  def createMissingOrg: Future[Seq[Future[Team]]] = {
    dataService.teams.allTeamsWithoutOrg.map { teams =>
      teams.map { team =>
        dataService.organizations.create(team.name).flatMap { org =>
          dataService.teams.setOrganizationIdFor(team, org.id)
        }
      }
    }
  }

  def run(): Unit = {
    dataService.runNow(createMissingOrg)
  }

  run()
}
