import java.io.File
import java.time.OffsetDateTime

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
import services.DataService
import support.TestContext
import utils.github.{GithubCommitterInfo, GithubPusher}

import scala.collection.JavaConverters._
import scala.concurrent.Future

class GithubPusherSpec extends PlaySpec with MockitoSugar with BeforeAndAfterAll {
  val uuid = IDs.next
  val temp = FileUtils.getTempDirectory
  val specParentDir = new File(temp, s"githubpusherspec-$uuid")
  val parentPath = specParentDir.getAbsolutePath
  val origin = new File(specParentDir, "repo")
  val behaviorGroupId = IDs.next
  val maybeBehaviorGroupExportId = Some(IDs.next)
  val behaviorGroupTimestamp = OffsetDateTime.now
  val behaviorId = IDs.next
  val maybeBehaviorExportId = Some(IDs.next)

  override def beforeAll(): Unit = {
    FileUtils.forceMkdir(origin)
    FileUtils.touch(new File(origin, "README"))

    val git: Git = Git.init().
      setDirectory(origin).
      setGitDir(new File(origin, ".git")).call()

    git.add().addFilepattern(".").call()
    git.commit().setAuthor("Test", "test@test.test").
      setMessage("Empty repo").call()
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

  def setupGroup(user: User, team: Team, dataService: DataService): BehaviorGroup = {
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
    when(dataService.messageTriggers.allFor(any[BehaviorVersion])).thenReturn(Future.successful(Seq()))
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

  "GithubPusher.run" should {
    "commit and push a skill to an empty repo" in new TestContext {
      running(app) {
        val group = setupGroup(user, team, dataService)
        val groupVersion = setupGroupVersionFor(group, dataService)
        val behavior = setupBehaviorFor(group, user, dataService)
        val actionName = "happyAction"
        val behaviorVersion = setupBehaviorVersionFor(actionName, behavior, groupVersion, dataService)

        val branchName = "master"
        await(GithubPusher(
          owner = "test",
          repoName = "test",
          branch = branchName,
          commitMessage = "First version of skill",
          repoAccessToken = "token",
          committerInfo = GithubCommitterInfo("Test", "test@test.test"),
          behaviorGroup = group,
          user = user,
          services = services,
          maybeRemoteUrlBuilder = Some((_, _) => origin.getAbsolutePath),
          ec = ec
        ).run)

        val git = Git.open(origin)
        git.reset().setMode(ResetType.HARD).call()

        val actionsDir = new File(origin, "actions")

        val actionsDirList = listDirsIn(actionsDir)
        actionsDirList mustBe Seq(actionName)
      }
    }

    "commit and push a skill with a renamed action to an existing repo" in new TestContext {
      running(app) {
        val group = setupGroup(user, team, dataService)
        val groupVersion = setupGroupVersionFor(group, dataService)
        val behavior = setupBehaviorFor(group, user, dataService)
        val actionName = "happierAction"
        val behaviorVersion = setupBehaviorVersionFor(actionName, behavior, groupVersion, dataService)

        val branchName = "renamed"

        await(GithubPusher(
          "test",
          "test",
          branchName,
          "Commit message",
          "token",
          GithubCommitterInfo("Test", "test@test.test"),
          group,
          user,
          services,
          Some((_, _) => origin.getAbsolutePath),
          ec
        ).run)

        val git = Git.open(origin)
        git.checkout().setName(branchName).call()

        val actionsDir = new File(origin, "actions")
        val actionsDirList = listDirsIn(actionsDir)
        actionsDirList mustBe Seq(actionName)

      }
    }
  }
}
