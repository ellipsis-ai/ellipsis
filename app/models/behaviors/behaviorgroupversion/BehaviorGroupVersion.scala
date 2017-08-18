package models.behaviors.behaviorgroupversion

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.team.Team
import utils.SafeFileName

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

  val functionName: String = id

  def isCurrentVersion: Boolean = group.maybeCurrentVersionId.contains(id)

  def exportName: String = {
    Option(SafeFileName.forName(name)).filter(_.nonEmpty).getOrElse(id)
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
