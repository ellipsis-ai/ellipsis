package json

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.team.Team
import services.DataService
import utils.{FuzzyMatchPattern, FuzzyMatchable, SimpleFuzzyMatchPattern}

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
                              createdAt: Option[OffsetDateTime]
                            ) extends Ordered[BehaviorGroupData] with FuzzyMatchable {

  val fuzzyMatchPatterns: Seq[FuzzyMatchPattern] = {
    Seq(fuzzyMatchName, fuzzyMatchDescription) ++ behaviorVersions.flatMap(_.triggers)
  }

  lazy val inputs = dataTypeInputs ++ actionInputs

  lazy val dataTypeBehaviorVersions = behaviorVersions.filter(_.isDataType)
  lazy val actionBehaviorVersions = behaviorVersions.filterNot(_.isDataType)

  val maybeNonEmptyName: Option[String] = name.map(_.trim).filter(_.nonEmpty)

  def copyForTeam(team: Team): BehaviorGroupData = {
    copy(behaviorVersions = behaviorVersions.map(_.copyForTeam(team)))
  }

  def copyForImportOf(group: BehaviorGroup): BehaviorGroupData = {
    val actionInputsWithIds = actionInputs.map(_.copyWithIdsEnsuredFor(group))
    val dataTypeInputsWithIds = dataTypeInputs.map(_.copyWithIdsEnsuredFor(group))
    val behaviorVersionsWithIds = behaviorVersions.map(_.copyWithIdsEnsuredForImport(group, actionInputsWithIds ++ dataTypeInputsWithIds))
    copyForNewVersionFor(group, actionInputsWithIds, dataTypeInputsWithIds, behaviorVersionsWithIds)
  }

  def copyForNewVersionOf(group: BehaviorGroup): BehaviorGroupData = {
    copyForNewVersionFor(group, actionInputs, dataTypeInputs, behaviorVersions)
  }

  def copyForMergedGroup(group: BehaviorGroup): BehaviorGroupData = {
    copyForNewVersionFor(group, actionInputs, dataTypeInputs, behaviorVersions)
  }

  private def copyForNewVersionFor(
                                    group: BehaviorGroup,
                                    actionInputsToUse: Seq[InputData],
                                    dataTypeInputsToUse: Seq[InputData],
                                    behaviorVersionsToUse: Seq[BehaviorVersionData]
                                  ): BehaviorGroupData = {
    val oldToNewIdMapping = collection.mutable.Map[String, String]()
    val actionInputsWithIds = actionInputsToUse.map(ea => ea.copyWithNewIdIn(oldToNewIdMapping))
    val dataTypeInputsWithIds = dataTypeInputsToUse.map(ea => ea.copyWithNewIdIn(oldToNewIdMapping))
    val behaviorVersionsWithIds = behaviorVersionsToUse.map(ea => ea.copyWithNewIdIn(oldToNewIdMapping))
    val dataTypeVersionsWithIds = behaviorVersionsWithIds.filter(_.isDataType)
    val actionInputsForNewVersion = actionInputsWithIds.map(_.copyWithParamTypeIdsIn(dataTypeVersionsWithIds, oldToNewIdMapping))
    val dataTypeInputsForNewVersion = dataTypeInputsWithIds.map(_.copyWithParamTypeIdsIn(dataTypeVersionsWithIds, oldToNewIdMapping))
    copy(
      id = Some(group.id),
      actionInputs = actionInputsForNewVersion,
      dataTypeInputs = dataTypeInputsForNewVersion,
      behaviorVersions = behaviorVersionsWithIds
    )
  }

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

  lazy val fuzzyMatchName: FuzzyMatchPattern = {
    SimpleFuzzyMatchPattern(name)
  }

  lazy val fuzzyMatchDescription: FuzzyMatchPattern = {
    SimpleFuzzyMatchPattern(description)
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

  def buildFor(version: BehaviorGroupVersion, user: User, dataService: DataService): Future[BehaviorGroupData] = {
    for {
      behaviors <- dataService.behaviors.allForGroup(version.group)
      versionsData <- Future.sequence(behaviors.map { ea =>
        BehaviorVersionData.maybeFor(ea.id, user, dataService, Some(version), ea.maybeExportId)
      }).map(_.flatten.sortBy { ea =>
        (ea.isDataType, ea.maybeFirstTrigger)
      })
      inputs <- dataService.inputs.allForGroupVersion(version)
      inputsData <- Future.sequence(inputs.map(ea => InputData.fromInput(ea, dataService)))
    } yield {
      val (dataTypeInputsData, actionInputsData) = inputsData.partition { ea =>
        versionsData.find(v => ea.inputId.exists(v.inputIds.contains)).exists(_.isDataType)
      }
      BehaviorGroupData(
        Some(version.group.id),
        version.team.id,
        Option(version.name).filter(_.trim.nonEmpty),
        version.maybeDescription,
        version.maybeIcon,
        actionInputsData,
        dataTypeInputsData,
        versionsData,
        None,
        version.group.maybeExportId,
        Some(version.createdAt)
      )
    }
  }

  def maybeFor(id: String, user: User, maybeGithubUrl: Option[String], dataService: DataService): Future[Option[BehaviorGroupData]] = {
    for {
      maybeGroup <- dataService.behaviorGroups.find(id)
      maybeLatestGroupVersion <- maybeGroup.flatMap { group =>
        group.maybeCurrentVersionId.map { versionId =>
          dataService.behaviorGroupVersions.findWithoutAccessCheck(versionId)
        }
      }.getOrElse(Future.successful(None))
      data <- maybeLatestGroupVersion.map { version =>
        buildFor(version, user, dataService).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield data
  }

}
