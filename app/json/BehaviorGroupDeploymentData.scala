package json

import java.time.OffsetDateTime

import models.behaviors.behaviorgroupdeployment.BehaviorGroupDeployment

case class BehaviorGroupDeploymentData(
                                       id: String,
                                       groupId: String,
                                       groupVersionId: String,
                                       comment: Option[String],
                                       createdAt: OffsetDateTime
                                     )

object BehaviorGroupDeploymentData {

  def fromDeployment(deployment: BehaviorGroupDeployment): BehaviorGroupDeploymentData = {
    BehaviorGroupDeploymentData(
      deployment.id,
      deployment.groupId,
      deployment.groupVersionId,
      deployment.maybeComment,
      deployment.createdAt
    )
  }
}
