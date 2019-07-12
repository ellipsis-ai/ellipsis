package json

import java.time.OffsetDateTime

import models.team.Team
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class SkillManifestConfig(containerId: String, csrfToken: String, isAdmin: Boolean, teamId: String, items: Seq[SkillManifestItemData])

object SkillManifestConfig {
  def buildForAction(containerId: String, csrfToken: String, isAdmin: Boolean, team: Team, dataService: DataService)
                    (implicit ec: ExecutionContext): DBIO[SkillManifestConfig] = {
    for {
      currentGroupVersions <- dataService.behaviorGroupVersions.allCurrentForTeamAction(team)
      firstGroupVersions <- dataService.behaviorGroupVersions.allFirstForTeamAction(team)
      firstDeployments <- dataService.behaviorGroupDeployments.firstForTeamAction(team)
      managedBehaviorGroups <- dataService.managedBehaviorGroups.allForAction(team)
      managedGroupUsers <- DBIO.sequence(managedBehaviorGroups.flatMap(_.maybeContactId).distinct.map { userId =>
        dataService.users.findAction(userId)
      }).map(_.flatten)
      lastGroupInvocations <- dataService.invocationLogEntries.lastForEachGroupForTeamAction(team)
      groupContactUserData <- {
        val desiredUsers = currentGroupVersions.flatMap { version =>
          val maybeManaged = managedBehaviorGroups.find(_.groupId == version.group.id)
          maybeManaged.flatMap { managed =>
            managedGroupUsers.find(mgu => managed.maybeContactId.contains(mgu.id))
          }.orElse {
            firstGroupVersions.find(fgv => fgv.group == version.group).flatMap(_.maybeAuthor)
          }
        }.distinct
        DBIO.sequence(desiredUsers.map { user =>
          dataService.users.userDataForAction(user, team)
        })
      }
    } yield {
      SkillManifestConfig(containerId, csrfToken, isAdmin, team.id, currentGroupVersions.map { groupVersion =>
        val group = groupVersion.group
        val maybeManagedGroup = managedBehaviorGroups.find(_.groupId == group.id)
        val maybeContact = groupContactUserData.find { gcud =>
          maybeManagedGroup.flatMap(_.maybeContactId).contains(gcud.ellipsisUserId) ||
            firstGroupVersions.find(_.group == group).flatMap(_.maybeAuthor.map(_.id)).contains(gcud.ellipsisUserId)
        }
        val maybeFirstDeployed = firstDeployments.find(_.groupId == group.id)
        val maybeLastInvoked = lastGroupInvocations.find(_.groupId == groupVersion.group.id).flatMap(_.maybeTimestamp)
        SkillManifestItemData(
          groupVersion.name,
          Some(group.id),
          maybeContact,
          groupVersion.maybeDescription.getOrElse(""),
          managed = maybeManagedGroup.isDefined,
          group.createdAt,
          maybeFirstDeployed.map(_.createdAt),
          maybeLastInvoked
        )
      }.sorted.reverse)
    }
  }

  def developmentStatusFor(maybeFirstDeployed: Option[OffsetDateTime]): String = {
    if (maybeFirstDeployed.isDefined) {
      "Production"
    } else {
      "Development"
    }
  }

  def buildFor(containerId: String, csrfToken: String, isAdmin: Boolean, team: Team, dataService: DataService)
              (implicit ec: ExecutionContext): Future[SkillManifestConfig] = {
    dataService.run(buildForAction(containerId, csrfToken, isAdmin, team, dataService))
  }
}
