package json

import java.time.OffsetDateTime

import models.accounts.user.User
import models.team.Team
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

case class BehaviorGroupData(
                              id: Option[String],
                              name: String,
                              description: String,
                              icon: Option[String],
                              behaviorVersions: Seq[BehaviorVersionData],
                              githubUrl: Option[String],
                              importedId: Option[String],
                              publishedId: Option[String],
                              createdAt: OffsetDateTime
                            ) extends Ordered[BehaviorGroupData] {

  val maybeNonEmptyName: Option[String] = Option(name.trim).filter(_.nonEmpty)

  def copyForTeam(team: Team): BehaviorGroupData = {
    copy(behaviorVersions = behaviorVersions.map(_.copyForTeam(team)))
  }

  def isMisc: Boolean = name == "Miscellaneous"

  lazy val sortedActionBehaviorVersions = {
    behaviorVersions.filterNot(_.isDataType).sortBy(_.maybeFirstTrigger)
  }

  lazy val maybeFirstActionBehaviorVersion: Option[BehaviorVersionData] = sortedActionBehaviorVersions.headOption
  lazy val maybeFirstTrigger: Option[String] = maybeFirstActionBehaviorVersion.flatMap(_.maybeFirstTrigger)

  lazy val maybeSortString: Option[String] = {
    if (!this.name.isEmpty) {
      Some(this.name.toLowerCase)
    } else {
      this.maybeFirstTrigger
    }
  }

  private def anyTriggerMatchesHelpSearch(regex: Regex): Boolean = {
    behaviorVersions.exists { version =>
      version.triggers.exists { trigger =>
        regex.findFirstMatchIn(trigger.text).isDefined
      }
    }
  }

  def matchesHelpSearch(helpSearch: String): Boolean = {
    val regex = ("(?i)" ++ helpSearch).r
    regex.findFirstMatchIn(name).isDefined ||
      regex.findFirstMatchIn(description).isDefined ||
      anyTriggerMatchesHelpSearch(regex)
  }

  import scala.math.Ordered.orderingToOrdered
  def compare(that: BehaviorGroupData): Int = {
    if (this.maybeNonEmptyName.isEmpty && that.maybeNonEmptyName.isDefined) {
      1
    } else if (this.maybeNonEmptyName.isDefined && that.maybeNonEmptyName.isEmpty) {
      -1
    } else if (this.maybeSortString.isEmpty && that.maybeSortString.isDefined) {
      1
    } else if (this.maybeSortString.isDefined && that.maybeSortString.isEmpty) {
      -1
    } else {
      this.maybeSortString compare that.maybeSortString
    }
  }
}

object BehaviorGroupData {

  def maybeFor(id: String, user: User, maybeGithubUrl: Option[String], dataService: DataService): Future[Option[BehaviorGroupData]] = {
    for {
      maybeGroup <- dataService.behaviorGroups.find(id)
      behaviors <- maybeGroup.map { group =>
        dataService.behaviors.allForGroup(group)
      }.getOrElse(Future.successful(Seq()))
      versionsData <- Future.sequence(behaviors.map { ea =>
        BehaviorVersionData.maybeFor(ea.id, user, dataService)
      }).map(_.flatten.sortBy { ea =>
        (ea.isDataType, ea.maybeFirstTrigger)
      })
    } yield maybeGroup.map { group =>
      BehaviorGroupData(
        Some(group.id),
        group.name,
        group.maybeDescription.getOrElse(""),
        None,
        versionsData,
        maybeGithubUrl,
        group.maybeImportedId,
        None,
        group.createdAt
      )
    }
  }

}
