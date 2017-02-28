package json

import java.time.OffsetDateTime

import models.accounts.user.User
import models.team.Team
import services.DataService
import utils.FuzzyMatchable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorGroupData(
                              id: Option[String],
                              teamId: String,
                              name: Option[String],
                              description: Option[String],
                              icon: Option[String],
                              actionInputs: Seq[InputData],
                              dataTypeInputs: Seq[InputData],
                              behaviorVersions: Seq[BehaviorVersionData],
                              githubUrl: Option[String],
                              exportId: Option[String],
                              createdAt: OffsetDateTime
                            ) extends Ordered[BehaviorGroupData] {

  val maybeNonEmptyName: Option[String] = name.map(_.trim).filter(_.nonEmpty)

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
    maybeNonEmptyName.map { nonEmptyName =>
      nonEmptyName.toLowerCase
    }.orElse(this.maybeFirstTrigger)
  }

  lazy val fuzzyMatchName: FuzzyMatchable = {
    FuzzyBehaviorGroupDetail(name.getOrElse(""))
  }

  lazy val fuzzyMatchDescription: FuzzyMatchable = {
    FuzzyBehaviorGroupDetail(description.getOrElse(""))
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

  private def inputsFor(versionsData: Seq[BehaviorVersionData], dataService: DataService) = {
    Future.sequence(versionsData.flatMap { version =>
      version.params.map { param =>
        param.inputId.map(dataService.inputs.find).getOrElse(Future.successful(None))
      }
    }).map(_.flatten)
  }

  private def inputsDataFor(versionsData: Seq[BehaviorVersionData], dataService: DataService) = {
    inputsFor(versionsData, dataService).flatMap { inputs =>
      Future.sequence(inputs.map(InputData.fromInput(_, dataService)))
    }
  }

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
      (dataTypeVersionsData, actionVersionsData) <- Future.successful(versionsData.partition(_.isDataType))
      dataTypeInputsData <- inputsDataFor(dataTypeVersionsData, dataService)
      actionInputsData <- inputsDataFor(actionVersionsData, dataService)
    } yield {
      maybeGroup.map { group =>
        BehaviorGroupData(
          Some(group.id),
          group.team.id,
          Some(group.name),
          group.maybeDescription,
          None,
          actionInputsData,
          dataTypeInputsData,
          versionsData,
          maybeGithubUrl,
          group.maybeExportId,
          group.createdAt
        )
      }
    }
  }

}

case class FuzzyBehaviorGroupDetail(text: String) extends FuzzyMatchable {
  val maybeFuzzyMatchPattern = Option(text).filter(_.trim.nonEmpty)
}
