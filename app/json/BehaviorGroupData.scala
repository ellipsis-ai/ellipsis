package json

import java.time.OffsetDateTime

import models.accounts.oauth2application.OAuth2Application
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.team.Team
import services.DataService
import services.caching.CacheService
import utils.{FuzzyMatchPattern, FuzzyMatchable, SimpleFuzzyMatchPattern}

import scala.concurrent.{ExecutionContext, Future}

case class BehaviorGroupData(
                              id: Option[String],
                              teamId: String,
                              name: Option[String],
                              description: Option[String],
                              icon: Option[String],
                              actionInputs: Seq[InputData],
                              dataTypeInputs: Seq[InputData],
                              behaviorVersions: Seq[BehaviorVersionData],
                              libraryVersions: Seq[LibraryVersionData],
                              requiredAWSConfigs: Seq[RequiredAWSConfigData],
                              requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfigData],
                              requiredSimpleTokenApis: Seq[RequiredSimpleTokenApiData],
                              githubUrl: Option[String],
                              gitSHA: Option[String],
                              exportId: Option[String],
                              createdAt: Option[OffsetDateTime],
                              author: Option[UserData],
                              deployment: Option[BehaviorGroupDeploymentData],
                              metaData: Option[BehaviorGroupMetaData],
                              isManaged: Boolean,
                              managedContact: Option[UserData]
                            ) extends Ordered[BehaviorGroupData] with FuzzyMatchable {

  val fuzzyMatchPatterns: Seq[FuzzyMatchPattern] = {
    Seq(fuzzyMatchName, fuzzyMatchDescription) ++ behaviorVersions.flatMap(_.triggers)
  }

  lazy val fuzzyMatchName: FuzzyMatchPattern = {
    SimpleFuzzyMatchPattern(name)
  }

  lazy val fuzzyMatchDescription: FuzzyMatchPattern = {
    SimpleFuzzyMatchPattern(description)
  }

  lazy val inputs = dataTypeInputs ++ actionInputs

  lazy val dataTypeBehaviorVersions = behaviorVersions.filter(_.isDataType)
  lazy val actionBehaviorVersions = behaviorVersions.filterNot(_.isDataType)

  val maybeNonEmptyName: Option[String] = name.map(_.trim).filter(_.nonEmpty)

  def copyForImportableForTeam(team: Team, maybeExistingGroupData: Option[BehaviorGroupData]): BehaviorGroupData = {
    val actionInputsWithIds = actionInputs.map(_.copyWithIdsEnsuredFor(maybeExistingGroupData))
    val dataTypeInputsWithIds = dataTypeInputs.map(_.copyWithIdsEnsuredFor(maybeExistingGroupData))
    val behaviorVersionsWithIds = behaviorVersions.map(_.copyForImportableForTeam(team, actionInputsWithIds ++ dataTypeInputsWithIds, maybeExistingGroupData))
    val libraryVersionsWithIds = libraryVersions.map(_.copyWithIdsEnsuredFor(maybeExistingGroupData))
    val actionInputsWithParamTypeIds = actionInputsWithIds.map(_.copyWithParamTypeIdFromExportId(behaviorVersionsWithIds))
    val dataTypeInputsWithParamTypeIds = dataTypeInputsWithIds.map(_.copyWithParamTypeIdFromExportId(behaviorVersionsWithIds))
    copy(
      id = maybeExistingGroupData.flatMap(_.id),
      actionInputs = actionInputsWithParamTypeIds,
      dataTypeInputs = dataTypeInputsWithParamTypeIds,
      behaviorVersions = behaviorVersionsWithIds,
      libraryVersions = libraryVersionsWithIds
    )
  }

  def copyForNewVersionOf(group: BehaviorGroup): BehaviorGroupData = {
    val oldToNewIdMapping = collection.mutable.Map[String, String]()
    val actionInputsWithIds = actionInputs.map(ea => ea.copyWithNewIdIn(oldToNewIdMapping))
    val dataTypeInputsWithIds = dataTypeInputs.map(ea => ea.copyWithNewIdIn(oldToNewIdMapping))
    val behaviorVersionsWithIds = behaviorVersions.map(ea => ea.copyWithNewIdIn(oldToNewIdMapping))
    val libraryVersionsWithIds = libraryVersions.map(ea => ea.copyWithNewIdIn(oldToNewIdMapping))
    val actionInputsForNewVersion = actionInputsWithIds.map(_.copyWithParamTypeIdsIn(oldToNewIdMapping))
    val dataTypeInputsForNewVersion = dataTypeInputsWithIds.map(_.copyWithParamTypeIdsIn(oldToNewIdMapping))
    val behaviorVersionsForNewVersion = behaviorVersionsWithIds.map(_.copyWithParamTypeIdsIn(oldToNewIdMapping))
    copy(
      id = Some(group.id),
      actionInputs = actionInputsForNewVersion,
      dataTypeInputs = dataTypeInputsForNewVersion,
      behaviorVersions = behaviorVersionsForNewVersion,
      libraryVersions = libraryVersionsWithIds
    )
  }

  def copyWithApiApplicationsIfAvailable(oauth2Applications: Seq[OAuth2Application]): BehaviorGroupData = {
    val oauth2 = requiredOAuth2ApiConfigs.map { eaRequired =>
      oauth2Applications.find { eaAvailable =>
        eaAvailable.api.id == eaRequired.apiId && eaRequired.recommendedScope == eaAvailable.maybeScope
      }.map { app =>
        eaRequired.copy(config = Some(OAuth2ApplicationData.from(app)))
      }.getOrElse(eaRequired)
    }
    copy(requiredOAuth2ApiConfigs = oauth2)
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

  def buildForImmutableData(
                             immutableData: ImmutableBehaviorGroupVersionData,
                             maybeInitialVersion: Option[BehaviorGroupVersion],
                             user: User,
                             dataService: DataService
                           )(implicit ec: ExecutionContext): Future[BehaviorGroupData] = {
    val versionId = immutableData.id
    for {
      requiredAWSConfigs <- dataService.requiredAWSConfigs.allForId(versionId)
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allForId(versionId)
      requiredSimpleTokenApis <- dataService.requiredSimpleTokenApis.allForId(versionId)
      maybeAuthor <- immutableData.authorId.map { authorId =>
        dataService.users.find(authorId)
      }.getOrElse(Future.successful(None))
      maybeTeam <- dataService.teams.find(immutableData.teamId)
      maybeUserData <- (for {
        author <- maybeAuthor
        team <- maybeTeam
      } yield {
        dataService.users.userDataFor(author, team).map(Some(_))
      }).getOrElse(Future.successful(None))
      maybeInitialUserData <- (for {
        initialAuthor <- maybeInitialVersion.flatMap(_.maybeAuthor)
        team <- maybeTeam
      } yield {
        dataService.users.userDataFor(initialAuthor, team).map(Some(_))
      }).getOrElse(Future.successful(None))
      maybeDeployment <- dataService.behaviorGroupDeployments.findForBehaviorGroupVersionId(versionId)
      maybeDeploymentData <- maybeDeployment.map { deployment =>
        BehaviorGroupDeploymentData.fromDeployment(deployment, dataService).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeBehaviorGroup <- dataService.behaviorGroups.findWithoutAccessCheck(immutableData.groupId)
      maybeManagedInfo <- (for {
        team <- maybeTeam
        group <- maybeBehaviorGroup
      } yield {
        dataService.managedBehaviorGroups.infoFor(group, team).map(Some(_))
      }).getOrElse(Future.successful(None))
    } yield {
      val maybeMetaData = maybeInitialVersion.map { initialVersion =>
        BehaviorGroupMetaData(initialVersion.group.id, initialVersion.createdAt, maybeInitialUserData)
      }
      BehaviorGroupData(
        Some(immutableData.groupId),
        immutableData.teamId,
        immutableData.name.filter(_.trim.nonEmpty),
        immutableData.description,
        immutableData.icon,
        immutableData.actionInputs,
        immutableData.dataTypeInputs,
        immutableData.behaviorVersions,
        immutableData.libraryVersions,
        requiredAWSConfigs.map(RequiredAWSConfigData.from),
        requiredOAuth2ApiConfigs.map(RequiredOAuth2ApiConfigData.from),
        requiredSimpleTokenApis.map(RequiredSimpleTokenApiData.from),
        None,
        None, // don't include SHA when building new data from existing version
        immutableData.exportId,
        immutableData.createdAt,
        maybeUserData,
        maybeDeploymentData,
        maybeMetaData,
        maybeManagedInfo.exists(_.isManaged),
        maybeManagedInfo.flatMap(_.maybeContactData)
      )
    }
  }

  def buildFor(
                version: BehaviorGroupVersion,
                user: User,
                maybeInitialVersion: Option[BehaviorGroupVersion],
                dataService: DataService,
                cacheService: CacheService
              )(implicit ec: ExecutionContext): Future[BehaviorGroupData] = {
    for {
      immutableData <- cacheService.getBehaviorGroupVersionData(version.id).map(Future.successful).getOrElse {
        for {
          behaviors <- dataService.behaviors.allForGroup(version.group)
          versionsData <- Future.sequence(behaviors.map { ea =>
            BehaviorVersionData.maybeFor(ea.id, user, dataService, Some(version), ea.maybeExportId)
          }).map(_.flatten.sortBy { ea =>
            (ea.isDataType, ea.maybeFirstTrigger)
          })
          inputs <- dataService.inputs.allForGroupVersion(version)
          inputsData <- Future.sequence(inputs.map(ea => InputData.fromInput(ea, dataService)))
          libraryVersions <- dataService.libraries.allFor(version)
          libraryVersionsData <- Future.successful(libraryVersions.map(ea => LibraryVersionData.fromVersion(ea)))
        } yield {
          val (dataTypeInputsData, actionInputsData) = inputsData.partition { ea =>
            versionsData.find(v => ea.inputId.exists(v.inputIds.contains)).exists(_.isDataType)
          }
          val immutable = ImmutableBehaviorGroupVersionData(
            version.id,
            version.group.id,
            version.team.id,
            version.maybeAuthor.map(_.id),
            Some(version.name),
            version.maybeDescription,
            version.maybeIcon,
            actionInputsData,
            dataTypeInputsData,
            versionsData,
            libraryVersionsData,
            version.group.maybeExportId,
            Some(version.createdAt)
          )
          cacheService.cacheBehaviorGroupVersionData(immutable)
          immutable
        }
      }
      data <- buildForImmutableData(immutableData, maybeInitialVersion, user, dataService)
    } yield data
  }

  def maybeFor(id: String, user: User, maybeGithubUrl: Option[String], dataService: DataService, cacheService: CacheService)(implicit ec: ExecutionContext): Future[Option[BehaviorGroupData]] = {
    for {
      maybeGroup <- dataService.behaviorGroups.find(id, user)
      maybeFirstGroupVersion <- maybeGroup.map { group =>
        dataService.behaviorGroupVersions.maybeFirstFor(group)
      }.getOrElse(Future.successful(None))
      maybeLatestGroupVersion <- maybeGroup.map { group =>
          dataService.behaviorGroupVersions.maybeCurrentFor(group)
      }.getOrElse(Future.successful(None))
      data <- maybeLatestGroupVersion.map { version =>
        buildFor(
          version,
          user,
          maybeFirstGroupVersion,
          dataService,
          cacheService
        ).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield data
  }

}
