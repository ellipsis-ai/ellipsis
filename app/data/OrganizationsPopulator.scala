package data


import javax.inject._

import models.IDs
import com.chargebee.models.Subscription
import models.organization.Organization
import models.team.Team
import services.DataService

import scala.concurrent.{ExecutionContext, Future}


class OrganizationsPopulator @Inject() (
                                     dataService: DataService,
                                     implicit val ec: ExecutionContext
                                   ) {

  def createMissingOrgs: Future[Seq[Organization]] = {
    for {
      teamsWithoutOrgs <- dataService.teams.allTeamsWithoutOrg
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
