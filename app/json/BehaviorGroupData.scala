package json

import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

import models.accounts.{OAuthApi, OAuthApplication}
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.team.Team
import services.DataService
import services.caching.CacheService
import slick.dbio.DBIO
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
                              requiredOAuthApiConfigs: Seq[RequiredOAuthApiConfigData],
                              requiredSimpleTokenApis: Seq[RequiredSimpleTokenApiData],
                              gitSHA: Option[String],
                              exportId: Option[String],
                              createdAt: Option[OffsetDateTime],
                              author: Option[UserData],
                              deployment: Option[BehaviorGroupDeploymentData],
                              metaData: Option[BehaviorGroupMetaData],
                              isManaged: Boolean,
                              managedContact: Option[UserData],
                              linkedGithubRepo: Option[LinkedGithubRepoData]
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

  private def requiredOAuthApiConfigsAction(findFn: String => DBIO[Option[OAuthApi]], dataService: DataService)(implicit ec: ExecutionContext): DBIO[Seq[RequiredOAuthApiConfigData]] = {
    DBIO.sequence(requiredOAuthApiConfigs.map { ea =>
      findFn(ea.apiId).map { maybeApi =>
        maybeApi.map(_ => ea)
      }
    }).map(_.flatten)
  }

  def requiredOAuth1ApiConfigsAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Seq[RequiredOAuthApiConfigData]] = {
    requiredOAuthApiConfigsAction(dataService.oauth1Apis.findAction, dataService)
  }

  def requiredOAuth2ApiConfigsAction(dataService: DataService)(implicit ec: ExecutionContext): DBIO[Seq[RequiredOAuthApiConfigData]] = {
    requiredOAuthApiConfigsAction(dataService.oauth2Apis.findAction, dataService)
  }

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
      libraryVersions = libraryVersionsWithIds,
      linkedGithubRepo = this.linkedGithubRepo orElse maybeExistingGroupData.flatMap(_.linkedGithubRepo)
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

  def copyWithApiApplicationsIfAvailable(oauthApplications: Seq[OAuthApplication]): BehaviorGroupData = {
    val required = requiredOAuthApiConfigs.map { eaRequired =>
      oauthApplications.find { eaAvailable =>
        eaAvailable.api.id == eaRequired.apiId
      }.map { app =>
        eaRequired.copy(config = Some(OAuthApplicationData.from(app)))
      }.getOrElse(eaRequired)
    }
    copy(requiredOAuthApiConfigs = required)
  }

  def withUnsavedNewBehavior(isDataType: Boolean, isTest: Boolean, maybeName: Option[String]): BehaviorGroupData = {
    copy(behaviorVersions = this.behaviorVersions :+ BehaviorVersionData.newUnsavedFor(this.teamId, isDataType, isTest, maybeName))
  }

  def withUnsavedClonedBehavior(behaviorIdToClone: String, maybeName: Option[String]): BehaviorGroupData = {
    val maybeBehaviorVersion = this.behaviorVersions.find(_.behaviorId.contains(behaviorIdToClone))
    val newInputs = maybeBehaviorVersion.map { behaviorVersion =>
      behaviorVersion.inputIds.flatMap { inputId =>
        this.inputs.find(_.inputId.contains(inputId)).map(_.copyForClone)
      }
    }.getOrElse(Seq.empty)
    val maybeNewBehaviorVersion = maybeBehaviorVersion.map { oldBehaviorVersion =>
      oldBehaviorVersion.copyForClone(newInputs.flatMap(_.inputId))
    }
    maybeNewBehaviorVersion.map { newBehaviorVersion =>
      val newBehaviorVersions = this.behaviorVersions :+ newBehaviorVersion
      if (newBehaviorVersion.isTest) {
        this.copy(behaviorVersions = newBehaviorVersions)
      } else if (newBehaviorVersion.isDataType) {
        this.copy(behaviorVersions = newBehaviorVersions, dataTypeInputs = this.dataTypeInputs ++ newInputs)
      } else {
        this.copy(behaviorVersions = newBehaviorVersions, actionInputs = this.actionInputs ++ newInputs)
      }
    }.getOrElse {
      this
    }
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

  def buildForImmutableDataAction(
                                   immutableData: ImmutableBehaviorGroupVersionData,
                                   maybeInitialVersion: Option[BehaviorGroupVersion],
                                   user: User,
                                   dataService: DataService
                                 )(implicit ec: ExecutionContext): DBIO[BehaviorGroupData] = {
    val versionId = immutableData.id
    for {
      requiredAWSConfigs <- dataService.requiredAWSConfigs.allForIdAction(versionId)
      requiredOAuth1ApiConfigs <- dataService.requiredOAuth1ApiConfigs.allForIdAction(versionId)
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allForIdAction(versionId)
      requiredSimpleTokenApis <- dataService.requiredSimpleTokenApis.allForIdAction(versionId)
      maybeAuthor <- immutableData.authorId.map { authorId =>
        dataService.users.findAction(authorId)
      }.getOrElse(DBIO.successful(None))
      maybeTeam <- dataService.teams.findAction(immutableData.teamId)
      maybeUserData <- (for {
        author <- maybeAuthor
        team <- maybeTeam
      } yield {
        dataService.users.userDataForAction(author, team).map(Some(_))
      }).getOrElse(DBIO.successful(None))
      maybeInitialUserData <- (for {
        initialAuthor <- maybeInitialVersion.flatMap(_.maybeAuthor)
        team <- maybeTeam
      } yield {
        dataService.users.userDataForAction(initialAuthor, team).map(Some(_))
      }).getOrElse(DBIO.successful(None))
      maybeDeployment <- dataService.behaviorGroupDeployments.findForBehaviorGroupVersionIdAction(versionId)
      maybeDeploymentData <- maybeDeployment.map { deployment =>
        BehaviorGroupDeploymentData.fromDeploymentAction(deployment, dataService).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeBehaviorGroup <- dataService.behaviorGroups.findWithoutAccessCheckAction(immutableData.groupId)
      maybeManagedInfo <- (for {
        team <- maybeTeam
        group <- maybeBehaviorGroup
      } yield {
        dataService.managedBehaviorGroups.infoForAction(group, team).map(Some(_))
      }).getOrElse(DBIO.successful(None))
      maybeLinkedGithubRepo <- maybeBehaviorGroup.map { group =>
        dataService.linkedGithubRepos.maybeForAction(group)
      }.getOrElse(DBIO.successful(None))
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
        (requiredOAuth1ApiConfigs ++ requiredOAuth2ApiConfigs).map(RequiredOAuthApiConfigData.from),
        requiredSimpleTokenApis.map(RequiredSimpleTokenApiData.from),
        None, // don't include SHA when building new data from existing version
        immutableData.exportId,
        immutableData.createdAt,
        maybeUserData,
        maybeDeploymentData,
        maybeMetaData,
        maybeManagedInfo.exists(_.isManaged),
        maybeManagedInfo.flatMap(_.maybeContactData),
        maybeLinkedGithubRepo.map(LinkedGithubRepoData.from)
      )
    }
  }

  def buildForImmutableData(
                             immutableData: ImmutableBehaviorGroupVersionData,
                             maybeInitialVersion: Option[BehaviorGroupVersion],
                             user: User,
                             dataService: DataService
                           )(implicit ec: ExecutionContext): Future[BehaviorGroupData] = {
    dataService.run(buildForImmutableDataAction(immutableData, maybeInitialVersion, user, dataService))
  }

  def buildForAction(
                      version: BehaviorGroupVersion,
                      user: User,
                      maybeInitialVersion: Option[BehaviorGroupVersion],
                      dataService: DataService,
                      cacheService: CacheService
                    )(implicit ec: ExecutionContext): DBIO[BehaviorGroupData] = {
    for {
      maybeCachedData <- DBIO.from(cacheService.getBehaviorGroupVersionData(version.id))
      immutableData <- maybeCachedData.map(DBIO.successful).getOrElse {
        for {
          behaviors <- dataService.behaviors.allForGroupAction(version.group)
          versionsData <- DBIO.sequence(behaviors.map { ea =>
            BehaviorVersionData.maybeForAction(ea.id, user, dataService, Some(version), ea.maybeExportId)
          }).map(_.flatten.sortBy { ea =>
            (ea.isDataType, ea.maybeFirstTrigger)
          })
          inputs <- dataService.inputs.allForGroupVersionAction(version)
          inputsData <- DBIO.sequence(inputs.map(ea => InputData.fromInputAction(ea, dataService)))
          libraryVersions <- dataService.libraries.allForAction(version)
          libraryVersionsData <- DBIO.successful(libraryVersions.map(ea => LibraryVersionData.fromVersion(ea)))
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
      data <- buildForImmutableDataAction(immutableData, maybeInitialVersion, user, dataService)
    } yield data
  }

  def buildFor(
                version: BehaviorGroupVersion,
                user: User,
                maybeInitialVersion: Option[BehaviorGroupVersion],
                dataService: DataService,
                cacheService: CacheService
              )(implicit ec: ExecutionContext): Future[BehaviorGroupData] = {
    dataService.run(buildForAction(version, user, maybeInitialVersion, dataService, cacheService))
  }

  def maybeFor(id: String, user: User, dataService: DataService, cacheService: CacheService)
              (implicit ec: ExecutionContext): Future[Option[BehaviorGroupData]] = {
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

  def createdAtFor(maybeTimestamp: Option[String]): OffsetDateTime = {
    maybeTimestamp.flatMap { timestamp =>
      try {
        Some(OffsetDateTime.parse(timestamp))
      } catch {
        case _: DateTimeParseException => None
      }
    }.getOrElse {
      OffsetDateTime.now
    }
  }

  def fromExport(
                  teamId: String,
                  maybeName: Option[String],
                  maybeDescription: Option[String],
                  maybeIcon: Option[String],
                  actionInputs: Seq[InputData],
                  dataTypeInputs: Seq[InputData],
                  behaviorVersions: Seq[BehaviorVersionData],
                  libraryVersions: Seq[LibraryVersionData],
                  requiredAWSConfigs: Seq[RequiredAWSConfigData],
                  requiredOAuthApiConfigs: Seq[RequiredOAuthApiConfigData],
                  requiredSimpleTokenApis: Seq[RequiredSimpleTokenApiData],
                  maybeGitSHA: Option[String],
                  maybeExportId: Option[String],
                  maybeAuthor: Option[UserData],
                  maybeLinkedGithubRepoData: Option[LinkedGithubRepoData],
                  maybeCreatedAt: Option[OffsetDateTime]
                ): BehaviorGroupData = {
    BehaviorGroupData(
      id = None,
      teamId,
      maybeName,
      maybeDescription,
      maybeIcon,
      actionInputs,
      dataTypeInputs,
      behaviorVersions,
      libraryVersions,
      requiredAWSConfigs,
      requiredOAuthApiConfigs,
      requiredSimpleTokenApis,
      maybeGitSHA,
      maybeExportId,
      createdAt = maybeCreatedAt,
      author = None,
      deployment = None,
      metaData = None,
      isManaged = false,
      managedContact = None,
      linkedGithubRepo = maybeLinkedGithubRepoData
    )
  }

  def forNewGroupFor(user: User, team: Team, dataService: DataService)(implicit ec: ExecutionContext): Future[BehaviorGroupData] = {
    dataService.users.userDataFor(user, team).map { userData =>
      BehaviorGroupData(
        id = None,
        teamId = team.id,
        name = None,
        description = None,
        icon = None,
        actionInputs = Seq(),
        dataTypeInputs = Seq(),
        behaviorVersions = Seq(),
        libraryVersions = Seq(),
        requiredAWSConfigs = Seq(),
        requiredOAuthApiConfigs = Seq(),
        requiredSimpleTokenApis = Seq(),
        gitSHA = None,
        exportId = None,
        Some(OffsetDateTime.now),
        Some(userData),
        deployment = None,
        metaData = None,
        isManaged = false,
        managedContact = None,
        linkedGithubRepo = None
      )
    }
  }

}
