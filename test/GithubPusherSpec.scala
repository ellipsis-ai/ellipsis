import java.io.File
import java.time.OffsetDateTime

import json.{BehaviorConfig, BehaviorGroupData, BehaviorVersionData}
import models.IDs
import models.accounts.user.User
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.{BehaviorVersion, Normal}
import models.behaviors.managedbehaviorgroup.ManagedBehaviorGroupInfo
import models.team.Team
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{NotFileFilter, TrueFileFilter}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import services.{DataService, DefaultServices}
import support.{BehaviorGroupDataBuilder, TestContext}
import utils.github.{GithubCommitterInfo, GithubPusher}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class GithubPusherSpec extends PlaySpec with MockitoSugar with BeforeAndAfterAll {
  val specParentDir = new File(FileUtils.getTempDirectory, s"githubpusherspec-${IDs.next}")
  val origin = new File(specParentDir, "repo")
  val behaviorGroupId = IDs.next
  val maybeBehaviorGroupExportId = Some(IDs.next)
  val behaviorGroupTimestamp = OffsetDateTime.now
  val behaviorId = IDs.next
  val maybeBehaviorExportId = Some(IDs.next)

  override def beforeAll(): Unit = {
    FileUtils.forceMkdir(origin)

    Git.init().
      setDirectory(origin).
      setGitDir(new File(origin, ".git")).call()
  }

  override def afterAll(): Unit = {
    FileUtils.deleteDirectory(specParentDir)
  }

  def listDirsIn(dir: File): Seq[String] = {
    val list = FileUtils.listFilesAndDirs(dir, new NotFileFilter(TrueFileFilter.INSTANCE), TrueFileFilter.INSTANCE)
    list.asScala.toSeq.
      filter(file => file != dir).
      map(file => file.getName)
  }

  def runPusherFor(group: BehaviorGroup, branchName: String, commitMessage: String, user: User, services: DefaultServices, ec: ExecutionContext): Unit = {
    await(GithubPusher(
      owner = "test",
      repoName = "test",
      branchName,
      commitMessage = "First version of skill",
      repoAccessToken = "token",
      GithubCommitterInfo("Test", "test@test.test"),
      group,
      user,
      services,
      maybeRemoteUrl = Some(origin.getAbsolutePath),
      ec
    ).run)
  }

  def setupGroup(user: User, team: Team, dataService: DataService)(implicit ec: ExecutionContext): BehaviorGroup = {
    val group = BehaviorGroup(behaviorGroupId, maybeBehaviorGroupExportId, team, behaviorGroupTimestamp)
    when(dataService.behaviorGroups.findWithoutAccessCheck(group.id)).thenReturn(Future.successful(Some(group)))
    when(dataService.behaviorGroups.find(group.id, user)).thenReturn(Future.successful(Some(group)))

    when(dataService.requiredAWSConfigs.allForId(any[String])).thenReturn(Future.successful(Seq()))
    when(dataService.requiredOAuth1ApiConfigs.allForId(any[String])).thenReturn(Future.successful(Seq()))
    when(dataService.requiredOAuth2ApiConfigs.allForId(any[String])).thenReturn(Future.successful(Seq()))
    when(dataService.requiredSimpleTokenApis.allForId(any[String])).thenReturn(Future.successful(Seq()))
    when(dataService.teams.find(team.id)).thenReturn(Future.successful(Some(team)))

    when(dataService.behaviorGroupDeployments.findForBehaviorGroupVersionId(any[String])).thenReturn(Future.successful(None))
    when(dataService.behaviorGroups.findWithoutAccessCheck(group.id)).thenReturn(Future.successful(Some(group)))
    when(dataService.managedBehaviorGroups.infoFor(group, team)).thenReturn(Future.successful(ManagedBehaviorGroupInfo(isManaged = false, None)))
    when(dataService.linkedGithubRepos.maybeFor(group)).thenReturn(Future.successful(None))

    when(dataService.behaviorGroupVersionSHAs.maybeCreateFor(any[BehaviorGroup], any[String])).thenReturn(Future.successful(None))

    group
  }

  def setupGroupVersionFor(group: BehaviorGroup, dataService: DataService): BehaviorGroupVersion = {
    val groupVersion = BehaviorGroupVersion(IDs.next, group, "Happy Skill", None, None, None, OffsetDateTime.now)
    when(dataService.behaviorGroups.maybeCurrentVersionFor(group)).thenReturn(Future.successful(Some(groupVersion)))
    when(dataService.behaviorGroupVersions.maybeFirstFor(group)).thenReturn(Future.successful(Some(groupVersion)))
    when(dataService.behaviorGroupVersions.maybeCurrentFor(group)).thenReturn(Future.successful(Some(groupVersion)))
    groupVersion
  }

  def setupBehaviorFor(group: BehaviorGroup, user: User, dataService: DataService): Behavior = {
    val behavior = Behavior(IDs.next, group.team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
    when(dataService.behaviors.allForGroup(group)).thenReturn(Future.successful(Seq(behavior)))
    when(dataService.behaviors.find(behavior.id, user)).thenReturn(Future.successful(Some(behavior)))
    behavior
  }

  def setupBehaviorVersionFor(name: String, behavior: Behavior, groupVersion: BehaviorGroupVersion, dataService: DataService): BehaviorVersion = {
    val behaviorVersion = BehaviorVersion(
      id = IDs.next,
      behavior = behavior,
      groupVersion = groupVersion,
      maybeDescription = None,
      maybeName = Some(name),
      maybeFunctionBody = Some(""),
      maybeResponseTemplate = Some(""),
      responseType = Normal,
      canBeMemoized = false,
      isTest = false,
      createdAt = OffsetDateTime.now
    )
    when(dataService.behaviorVersions.findFor(behavior, groupVersion)).thenReturn(Future.successful(Some(behaviorVersion)))
    when(dataService.behaviors.maybeCurrentVersionFor(behavior)).thenReturn(Future.successful(Some(behaviorVersion)))
    when(dataService.behaviorVersions.findWithoutAccessCheck(behaviorVersion.id)).thenReturn(Future.successful(Some(behaviorVersion)))

    when(dataService.behaviorParameters.allFor(any[BehaviorVersion])).thenReturn(Future.successful(Seq()))
    when(dataService.triggers.allFor(any[BehaviorVersion])).thenReturn(Future.successful(Seq()))
    when(dataService.dataTypeConfigs.maybeFor(any[BehaviorVersion])).thenReturn(Future.successful(None))
    when(dataService.inputs.allForGroupVersion(any[BehaviorGroupVersion])).thenReturn(Future.successful(Seq()))
    when(dataService.libraries.allFor(any[BehaviorGroupVersion])).thenReturn(Future.successful(Seq()))
    when(dataService.behaviorVersions.maybeFunctionFor(any[BehaviorVersion])).thenReturn(Future.successful(Some(
      """function(ellipsis) {
        |
        |}"""
    )))

    behaviorVersion
  }

  def setupGroupDataFor(
                         team: Team,
                         user: User,
                         behaviorVersion: BehaviorVersion,
                         dataService: DataService
                       )(implicit ec: ExecutionContext): BehaviorGroupData = {
    val groupData = BehaviorGroupDataBuilder.buildFor(
      team.id,
      maybeGroupId = Some(behaviorGroupId),
      maybeActions = Some(Seq(
        BehaviorVersionData(
          Some(behaviorVersion.id),
          team.id,
          Some(behaviorVersion.behavior.id),
          Some(behaviorVersion.group.id),
          isNew = None,
          name = behaviorVersion.maybeName,
          description = behaviorVersion.maybeDescription,
          functionBody = behaviorVersion.functionBody,
          responseTemplate = behaviorVersion.maybeResponseTemplate.getOrElse(""),
          inputIds = Seq(),
          triggers = Seq(),
          config = BehaviorConfig(
            behaviorVersion.maybeExportId,
            behaviorVersion.maybeName,
            behaviorVersion.responseType.id,
            canBeMemoized = None,
            isDataType = false,
            isTest = None,
            dataTypeConfig = None
          ),
          behaviorVersion.maybeExportId,
          Some(behaviorVersion.createdAt)
        )
      )),
      maybeDataTypes = Some(Seq())
    )
    when(dataService.behaviorGroups.maybeDataFor(behaviorGroupId, user)).thenReturn(Future.successful(Some(groupData)))
    groupData
  }

  "GithubPusher.run" should {
    "commit and push a skill to an empty repo, then push again with a renamed action" in new TestContext {
      running(app) {
        val originalGroup = setupGroup(user, team, dataService)
        val originalGroupVersion = setupGroupVersionFor(originalGroup, dataService)
        val originalBehavior = setupBehaviorFor(originalGroup, user, dataService)
        val originalActionName = "happyAction"
        val originalBehaviorVersion = setupBehaviorVersionFor(originalActionName, originalBehavior, originalGroupVersion, dataService)
        val groupData = setupGroupDataFor(team, user, originalBehaviorVersion, dataService)

        runPusherFor(originalGroup, "master", "First version of skill", user, services, ec)

        val git = Git.open(origin)
        git.reset().setMode(ResetType.HARD).call()

        val actionsDir = new File(origin, "actions")

        listDirsIn(actionsDir) mustBe Seq(originalActionName)

        val modifiedGroup = setupGroup(user, team, dataService)
        val modifiedGroupVersion = setupGroupVersionFor(modifiedGroup, dataService)
        val modifiedBehavior = setupBehaviorFor(modifiedGroup, user, dataService)
        val modifiedActionName = "happierAction"
        val modifiedBehaviorVersion = setupBehaviorVersionFor(modifiedActionName, modifiedBehavior, modifiedGroupVersion, dataService)
        val modifiedGroupData = setupGroupDataFor(team, user, modifiedBehaviorVersion, dataService)

        val modifiedBranch = "renamed"

        runPusherFor(modifiedGroup, modifiedBranch, "Renamed an action", user, services, ec)

        git.checkout().setName(modifiedBranch).call()

        listDirsIn(actionsDir) mustBe Seq(modifiedActionName)

      }
    }
  }

  new TestContext {
    running(app) {
      "GithubPusher.remoteUrl" should {
        "be constructed from the owner and repo for GitHub by default" in {
          GithubPusher(
            "ellipsis-ai",
            "give_kudos",
            "master",
            "Nothing to commit here",
            "token",
            GithubCommitterInfo("Tester McTesterson", "test@test.test"),
            setupGroup(user, team, dataService),
            user,
            services,
            None,
            ec
          ).remoteUrl mustBe "https://github.com/ellipsis-ai/give_kudos.git"
        }

        "use the URL provided if set" in {
          GithubPusher(
            "ellipsis-ai",
            "give_kudos",
            "master",
            "Nothing to commit here",
            "token",
            GithubCommitterInfo("Tester McTesterson", "test@test.test"),
            setupGroup(user, team, dataService),
            user,
            services,
            Some("https://nowhere.com/some/repo"),
            ec
          ).remoteUrl mustBe "https://nowhere.com/some/repo"
        }
      }
    }
  }
}
