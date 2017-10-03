package data

import java.time.OffsetDateTime
import javax.inject._

import json.{BehaviorGroupData, UserData}
import models.accounts.linkedaccount.LinkedAccount
import models.behaviors.builtins.ListScheduledBehavior
import models.team.Team
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

class BuiltinBehaviorPopulator @Inject() (
                                     dataService: DataService,
                                     implicit val ec: ExecutionContext
                                   ) {

  // update to create a new version
  val groupVersionId: String = "builtin-behavior-group-v1"

  def dataFor(team: Team, userData: UserData) = BehaviorGroupData(
    Some(groupVersionId),
    team.id,
    name = Some("Builtins"),
    description = None,
    icon = None,
    actionInputs = Seq(),
    dataTypeInputs = Seq(),
    Seq(
      ListScheduledBehavior.newVersionDataFor(team, dataService)
    ),
    Seq(),
    Seq(),
    Seq(),
    Seq(),
    githubUrl = None,
    exportId = None,
    Some(OffsetDateTime.now),
    Some(userData)
  )

  def createNewVersion(): DBIO[Unit] = {
    for {
      maybeAdminTeamId <- dataService.slackBotProfiles.allForSlackTeamIdAction(LinkedAccount.ELLIPSIS_SLACK_TEAM_ID).map(_.headOption.map(_.teamId))
      maybeAdminTeam <- maybeAdminTeamId.map { adminTeamId =>
        dataService.teams.findAction(adminTeamId)
      }.getOrElse(DBIO.successful(None))
      maybeBehaviorGroup <- maybeAdminTeam.map { adminTeam =>
        dataService.behaviorGroups.createForAction(None, adminTeam, isBuiltin = true).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeAdminUser <- maybeAdminTeam.map { adminTeam =>
        dataService.users.allForAction(adminTeam).map(_.headOption)
      }.getOrElse(DBIO.successful(None))
      _ <- (for {
        behaviorGroup <- maybeBehaviorGroup
        adminUser <- maybeAdminUser
        adminTeam <- maybeAdminTeam
      } yield {
        dataService.users.userDataForAction(adminUser, adminTeam).flatMap { userData =>
          dataService.behaviorGroupVersions.createForAction(
            behaviorGroup,
            adminUser,
            dataFor(adminTeam, userData)
          )
        }
      }).getOrElse(DBIO.successful({}))
    } yield {}
  }

  def run(): Unit = {
    dataService.runNow(dataService.behaviorGroupVersions.findWithoutAccessCheckAction(groupVersionId).flatMap { maybeExisting =>
      if (maybeExisting.isEmpty) {
        createNewVersion()
      } else {
        DBIO.successful({})
      }
    })
  }

  run()
}
