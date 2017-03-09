package json

import java.time.OffsetDateTime

import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
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
                              createdAt: Option[OffsetDateTime]
                            ) extends Ordered[BehaviorGroupData] {

  lazy val inputs = dataTypeInputs ++ actionInputs

  lazy val dataTypeBehaviorVersions = behaviorVersions.filter(_.isDataType)
  lazy val actionBehaviorVersions = behaviorVersions.filterNot(_.isDataType)

  val maybeNonEmptyName: Option[String] = name.map(_.trim).filter(_.nonEmpty)

  def copyForTeam(team: Team): BehaviorGroupData = {
    copy(behaviorVersions = behaviorVersions.map(_.copyForTeam(team)))
  }

  def copyForImportOf(group: BehaviorGroup): BehaviorGroupData = {
    val behaviorVersionsWithIds = behaviorVersions.map(_.copyWithIdsEnsuredForImport(group))
    copyForNewVersionFor(group, behaviorVersionsWithIds)
  }

  def copyForNewVersionOf(group: BehaviorGroup): BehaviorGroupData = {
    copyForNewVersionFor(group, behaviorVersions)
  }

  def copyForMergedGroup(group: BehaviorGroup): BehaviorGroupData = {
    copyForNewVersionFor(group, behaviorVersions.map(_.copyWithIdsEnsuredForMerge(group)))
  }

  private def copyForNewVersionFor(
                                    group: BehaviorGroup,
                                    behaviorVersionsToUse: Seq[BehaviorVersionData]
                                  ): BehaviorGroupData = {
    val behaviorVersionsWithEnsuredInputIds = behaviorVersionsToUse.map(ea => ea.copyWithEnsuredInputIds)
    val constructedDataTypeInputs = behaviorVersionsWithEnsuredInputIds.filter(_.isDataType).flatMap(_.params.map(_.inputData)).distinct
    val constructedActionInputs = behaviorVersionsWithEnsuredInputIds.filterNot(_.isDataType).flatMap(_.params.map(_.inputData)).distinct
    val oldToNewIdMapping = collection.mutable.Map[String, String]()
    val actionInputsWithIds = constructedActionInputs.map(ea => ea.copyWithNewIdIn(oldToNewIdMapping))
    val dataTypeInputsWithIds = constructedDataTypeInputs.map(ea => ea.copyWithNewIdIn(oldToNewIdMapping))
    val behaviorVersionsWithIds = behaviorVersionsWithEnsuredInputIds.map(ea => ea.copyWithNewIdIn(oldToNewIdMapping))
    val dataTypeVersionsWithIds = behaviorVersionsWithIds.filter(_.isDataType)
    val actionInputsForNewVersion = actionInputsWithIds.map(_.copyWithParamTypeIdsIn(dataTypeVersionsWithIds, oldToNewIdMapping))
    val dataTypeInputsForNewVersion = dataTypeInputsWithIds.map(_.copyWithParamTypeIdsIn(dataTypeVersionsWithIds, oldToNewIdMapping))
    val behaviorVersionsForNewVersion = behaviorVersionsWithIds.map(_.copyWithInputIdsIn(actionInputsForNewVersion ++ dataTypeInputsForNewVersion, oldToNewIdMapping))
    copy(
      id = Some(group.id),
      actionInputs = actionInputsForNewVersion,
      dataTypeInputs = dataTypeInputsForNewVersion,
      behaviorVersions = behaviorVersionsForNewVersion
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

  lazy val fuzzyMatchName: FuzzyMatchable = {
    FuzzyBehaviorGroupDetail(name)
  }

  lazy val fuzzyMatchDescription: FuzzyMatchable = {
    FuzzyBehaviorGroupDetail(description)
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
        param.inputVersionId.map(dataService.inputs.find).getOrElse(Future.successful(None))
      }
    }).map(_.flatten)
  }

  private def inputsDataFor(versionsData: Seq[BehaviorVersionData], dataService: DataService) = {
    inputsFor(versionsData, dataService).flatMap { inputs =>
      Future.sequence(inputs.map(InputData.fromInput(_, dataService)))
    }
  }

  def buildFor(version: BehaviorGroupVersion, user: User, dataService: DataService): Future[BehaviorGroupData] = {
    for {
      teamAccess <- dataService.users.teamAccessFor(user, Some(version.team.id))
      behaviors <- dataService.behaviors.allForGroup(version.group)
      versionsData <- Future.sequence(behaviors.map { ea =>
        BehaviorVersionData.maybeFor(ea.id, user, dataService, Some(version))
      }).map(_.flatten.sortBy { ea =>
        (ea.isDataType, ea.maybeFirstTrigger)
      })
      (dataTypeVersionsData, actionVersionsData) <- Future.successful(versionsData.partition(_.isDataType))
      dataTypeInputsData <- inputsDataFor(dataTypeVersionsData, dataService)
      actionInputsData <- inputsDataFor(actionVersionsData, dataService)
    } yield {
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

case class FuzzyBehaviorGroupDetail(maybeText: Option[String]) extends FuzzyMatchable {
  val maybeFuzzyMatchPattern = maybeText.filter(_.trim.nonEmpty)
}
