package data


import javax.inject._
import models.organization.Organization
import models.team.Team
import services.DataService
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger

class OrganizationsPopulator @Inject() (
                                     dataService: DataService,
                                     implicit val ec: ExecutionContext
                                   ) {

  def createMissingOrgs: Future[Seq[Organization]] = {
    for {
      teamsWithoutOrgs <- dataService.teams.allTeamsWithoutOrg.map { teams =>
        if (teams.length > 0 ) {
          Logger.info(s"Teams without Orgs: ${teams.length}.")
          Logger.info(s"Creating ${teams.length} organizations and adding the teams to them.")
        }
        teams
      }
      organizations <- Future.sequence(teamsWithoutOrgs.map(createOrg(_)))
    } yield organizations
  }

  private def createOrg(team:Team): Future[Organization] = {
    for {
      org <- dataService.organizations.create(team.name)
      teamWithOrg <- dataService.teams.setOrganizationIdFor(team, Some(org.id))
    } yield org
  }

  def run(): Unit = {
    dataService.runNow(createMissingOrgs)
  }

  run()

}
