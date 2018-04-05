package models.behaviors

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.Event
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class DeveloperContext(
                             isForUndeployedBehaviorVersion: Boolean,
                             hasUndeployedBehaviorVersionForAuthor: Boolean,
                             isInDevMode: Boolean
                           )

object DeveloperContext {
  def default: DeveloperContext = DeveloperContext(
    isForUndeployedBehaviorVersion = false,
    hasUndeployedBehaviorVersionForAuthor = false,
    isInDevMode = false
  )

  def buildFor(event: Event, behaviorVersion: BehaviorVersion, dataService: DataService)
              (implicit ec: ExecutionContext): DBIO[DeveloperContext] = {
    for {
      isForUndeployed <- dataService.behaviorGroupDeployments.findForBehaviorGroupVersionAction(behaviorVersion.groupVersion).map(_.isEmpty)
      user <- event.ensureUserAction(dataService)
      hasUndeployedVersionForAuthor <- dataService.behaviorGroupDeployments.hasUndeployedVersionForAuthorAction(behaviorVersion.groupVersion, user)
      isInDevMode <- dataService.devModeChannels.isEnabledForAction(event, behaviorVersion)
    } yield {
      DeveloperContext(isForUndeployed, hasUndeployedVersionForAuthor, isInDevMode)
    }
  }
}
