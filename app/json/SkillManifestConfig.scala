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
      behaviorGroups <- dataService.behaviorGroups.allForAction(team)
      firstAuthors <- DBIO.sequence(behaviorGroups.map { group =>
        dataService.behaviorGroupVersions.maybeFirstForAction(group).map { maybeVersion =>
          maybeVersion.map { version =>
            (group, version.maybeAuthor)
          }
        }
      }).map(_.flatten)
      behaviorGroupVersions <- DBIO.sequence(behaviorGroups.map { group =>
        dataService.behaviorGroupVersions.maybeCurrentForAction(group)
      }).map(_.flatten)
      firstDeployments <- DBIO.sequence(behaviorGroups.map { group =>
        dataService.behaviorGroupDeployments.maybeFirstForAction(group).map { maybeDeployment =>
          (group, maybeDeployment)
        }
      })
      managedBehaviorGroups <- dataService.managedBehaviorGroups.allForAction(team)
      groupContactUsers <- DBIO.sequence(behaviorGroups.map { group =>
        val maybeManagedContactId = managedBehaviorGroups.find(_.groupId == group.id).flatMap(_.maybeContactId)
        val maybeFirstAuthor = firstAuthors.find(_._1 == group).flatMap(_._2)
        maybeManagedContactId.map { userId =>
          dataService.users.findAction(userId)
        }.getOrElse {
          DBIO.successful(maybeFirstAuthor)
        }.map { maybeUser =>
          (group, maybeUser)
        }
      })
      groupContactUserData <- DBIO.sequence(groupContactUsers.map { case(group, maybeUser) =>
        maybeUser.map { user =>
          dataService.users.userDataForAction(user, team).map { userData =>
            (group, Some(userData))
          }
        }.getOrElse(DBIO.successful((group, None)))
      })
      lastGroupInvocations <- DBIO.sequence(behaviorGroups.map { group =>
        dataService.invocationLogEntries.lastForGroupAction(group).map { maybeOffsetDateTime =>
          (group, maybeOffsetDateTime)
        }
      })
    } yield {
      SkillManifestConfig(containerId, csrfToken, isAdmin, team.id, behaviorGroupVersions.map { groupVersion =>
        val group = groupVersion.group
        val maybeManagedGroup = managedBehaviorGroups.find(_.groupId == group.id)
        val maybeManagedContact = groupContactUserData.find(_._1 == group).flatMap(_._2)
        val maybeFirstDeployed = firstDeployments.find(_._1 == group).flatMap(_._2)
        val maybeLastInvoked = lastGroupInvocations.find(_._1 == group).flatMap(_._2)
        SkillManifestItemData(
          groupVersion.name,
          Some(groupVersion.group.id),
          maybeManagedContact,
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
