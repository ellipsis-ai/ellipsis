package models.behaviors.behaviorgroupversion

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team
import services.DataService
import utils.SafeFileName

import scala.concurrent.{ExecutionContext, Future}

case class BehaviorGroupVersion(
                                id: String,
                                group: BehaviorGroup,
                                name: String,
                                maybeIcon: Option[String],
                                maybeDescription: Option[String],
                                maybeAuthor: Option[User],
                                createdAt: OffsetDateTime
                              ) {

  val team: Team = group.team

  val functionName: String = BehaviorGroupVersion.functionNameFor(id)

  def exportName: String = {
    Option(SafeFileName.forName(name)).filter(_.nonEmpty).getOrElse(id)
  }

  private def isAllowedBecauseAdmin(user: User, dataService: DataService)(implicit ec: ExecutionContext): Future[Boolean] = {
    dataService.users.isAdmin(user)
  }

  private def isAllowedBecauseSameTeam(user: User, dataService: DataService)(implicit ec: ExecutionContext): Future[Boolean] = {
    for {
      maybeAttemptingUserSlackTeamId <- dataService.users.maybeSlackTeamIdFor(user)
      slackTeamIds <- dataService.slackBotProfiles.allFor(team).map(_.map(_.slackTeamId))
    } yield {
      maybeAttemptingUserSlackTeamId.exists { attemptingUserSlackTeamId =>
        slackTeamIds.contains(attemptingUserSlackTeamId)
      }
    }
  }

  def canBeTriggeredBy(user: User, dataService: DataService)(implicit ec: ExecutionContext): Future[Boolean] = {
    for {
      admin <- isAllowedBecauseAdmin(user, dataService)
      sameTeam <- isAllowedBecauseSameTeam(user, dataService)
    } yield {
      sameTeam || admin
    }
  }

  def toRaw: RawBehaviorGroupVersion = {
    RawBehaviorGroupVersion(
      id,
      group.id,
      name,
      maybeIcon,
      maybeDescription,
      maybeAuthor.map(_.id),
      createdAt
    )
  }

}

object BehaviorGroupVersion {
  val lambdaFunctionPrefix = "behavior-"
  def functionNameFor(groupVersionId: String): String = s"$lambdaFunctionPrefix$groupVersionId"
}
