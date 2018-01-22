package models.accounts.registration


import javax.inject.Inject
import services.DataService
import models.team.Team
import scala.concurrent.{ExecutionContext, Future}


class RegistrationServiceImpl @Inject() (
                                          val dataService: DataService,
                                          implicit val ec: ExecutionContext
                                        ) extends RegistrationService {

  def registerNewTeam(name: String): Future[Option[Team]] = {
    for {
      org <- dataService.organizations.create(name)
      team <- dataService.teams.create(name, org)
      sub <- dataService.subscriptions.createFreeSubscription(team,org)
    } yield {
      // TODO: how do stop the registration in case the createFreeSubscription fails?
      Some(team)
    }
  }

}


