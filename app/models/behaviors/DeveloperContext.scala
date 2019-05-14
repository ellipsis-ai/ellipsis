package models.behaviors

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.events.Event
import models.behaviors.testing.TestMessageEvent
import play.api.Configuration
import services.DataService
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class DevUrls(helpUrl: String, maybeEditUrl: Option[String])

case class DeveloperContext(
                             maybeBehaviorVersion: Option[BehaviorVersion],
                             isForUndeployedBehaviorVersion: Boolean,
                             hasUndeployedBehaviorVersionForAuthor: Boolean,
                             isInDevMode: Boolean,
                             isInInvocationTester: Boolean
                           ) {
  def maybeDevModeText(configuration: Configuration, teamIdForContext: String, botName: String): Option[String] = {
    val baseUrl = configuration.get[String]("application.apiBaseUrl")
    val helpUrl = baseUrl + controllers.routes.HelpController.devMode(Some(teamIdForContext), Some(botName)).url
    val maybeEditUrl = maybeBehaviorVersion.map { behaviorVersion =>
      baseUrl + controllers.routes.BehaviorEditorController.edit(behaviorVersion.groupVersion.id, Some(behaviorVersion.behavior.id), None).url
    }
    if (isForUndeployedBehaviorVersion) {
      val editLink = maybeEditUrl.map { url =>
        s"[✎ Edit](${url}) · "
      }.getOrElse("")
      val helpLink = s"[Info]($helpUrl)"
      Some(s"\uD83D\uDEA7 _Skill in development_ · ${editLink}${helpLink}")
    } else if (hasUndeployedBehaviorVersionForAuthor) {
      val editLink = maybeEditUrl.map { url =>
        s"[this skill](${url})"
      }.getOrElse("this skill")
      val helpLink = s"[dev mode]($helpUrl)"
      Some(s"\uD83D\uDEA7 You are running the deployed version of ${editLink} even though you’ve made changes. You can always use the most recent version in $helpLink.")
    } else {
      None
    }
  }
}

object DeveloperContext {
  def default: DeveloperContext = DeveloperContext(
    maybeBehaviorVersion = None,
    isForUndeployedBehaviorVersion = false,
    hasUndeployedBehaviorVersionForAuthor = false,
    isInDevMode = false,
    isInInvocationTester = false
  )

  def buildFor(event: Event, behaviorVersion: BehaviorVersion, dataService: DataService)
              (implicit ec: ExecutionContext): DBIO[DeveloperContext] = {
    for {
      isForUndeployed <- dataService.behaviorGroupDeployments.findForBehaviorGroupVersionAction(behaviorVersion.groupVersion).map(_.isEmpty)
      user <- event.ensureUserAction(dataService)
      hasUndeployedVersionForAuthor <- dataService.behaviorGroupDeployments.hasUndeployedVersionForAuthorAction(behaviorVersion.groupVersion, user)
      isInDevMode <- dataService.devModeChannels.isEnabledForAction(event, behaviorVersion)
    } yield {
      val isInInvocationTester = event match {
        case _: TestMessageEvent => true
        case _ => false
      }
      DeveloperContext(Some(behaviorVersion), isForUndeployed, hasUndeployedVersionForAuthor, isInDevMode, isInInvocationTester)
    }
  }
}
