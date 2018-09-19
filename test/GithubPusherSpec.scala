import java.io.File
import java.time.OffsetDateTime

import scala.collection.JavaConverters._
import export.BehaviorGroupExporter
import json._
import models.IDs
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.{BehaviorResponseType, BehaviorVersion, Normal}
import models.behaviors.managedbehaviorgroup.ManagedBehaviorGroupInfo
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{NameFileFilter, NotFileFilter, TrueFileFilter}
import org.eclipse.jgit.api.Git
import org.scalatest.mockito.MockitoSugar
import org.mockito.Matchers._
import org.scalatestplus.play.PlaySpec
import support.TestContext
import utils.github.{GithubCommitterInfo, GithubPusher}
import org.mockito.Mockito._
import play.api.test.Helpers._

import scala.concurrent.Future

class GithubPusherSpec extends PlaySpec with MockitoSugar {

  def listDirsIn(dir: File): Seq[String] = {
    FileUtils.listFilesAndDirs(dir, new NotFileFilter(TrueFileFilter.INSTANCE), TrueFileFilter.INSTANCE).
      asScala.toSeq.
      filter(file => file != dir).
      map(file => file.getName)
  }

  "GithubPusher.run" should {
    "commit and push, handling renaming an action" in new TestContext {
      running(app) {
        val group = BehaviorGroup(IDs.next, Some(IDs.next), team, OffsetDateTime.now)
        val groupVersion1 = BehaviorGroupVersion(IDs.next, group, "Happy Skill", None, None, None, OffsetDateTime.now)
        val groupVersion2 = groupVersion1.copy(createdAt = OffsetDateTime.now)

        val behavior = Behavior(IDs.next, team, Some(group), Some(IDs.next), isDataType = false, OffsetDateTime.now)
        val behaviorVersion1Name = "happyAction"
        val behaviorVersion1 = BehaviorVersion(
          id = IDs.next,
          behavior = behavior,
          groupVersion = groupVersion1,
          maybeDescription = None,
          maybeName = Some(behaviorVersion1Name),
          maybeFunctionBody = Some(""),
          maybeResponseTemplate = Some(""),
          responseType = Normal,
          canBeMemoized = false,
          isTest = false,
          createdAt = OffsetDateTime.now
        )
        val behaviorVersion2Name = "happierAction"
        val behaviorVersion2 = behaviorVersion1.copy(maybeName = Some(behaviorVersion2Name))

        when(dataService.behaviorGroups.findWithoutAccessCheck(group.id)).thenReturn(Future.successful(Some(group)))
        when(dataService.behaviorGroups.maybeCurrentVersionFor(group)).thenReturn(
          Future.successful(Some(groupVersion1)),
          Future.successful(Some(groupVersion2))
        )

        when(dataService.behaviorGroups.find(group.id, user)).thenReturn(Future.successful(Some(group)))
        when(dataService.behaviorGroupVersions.maybeFirstFor(group)).thenReturn(Future.successful(Some(groupVersion1)))
        when(dataService.behaviorGroupVersions.maybeCurrentFor(group)).thenReturn(
          Future.successful(Some(groupVersion1)),
          Future.successful(Some(groupVersion2))
        )

        when(dataService.behaviors.allForGroup(group)).thenReturn(Future.successful(Seq(behavior)))
        when(dataService.behaviors.find(behavior.id, user)).thenReturn(Future.successful(Some(behavior)))
        when(dataService.behaviorVersions.findFor(behavior, groupVersion1)).thenReturn(Future.successful(Some(behaviorVersion1)))
        when(dataService.behaviorVersions.findFor(behavior, groupVersion2)).thenReturn(Future.successful(Some(behaviorVersion2)))

        when(dataService.behaviors.maybeCurrentVersionFor(behavior)).thenReturn(
          Future.successful(Some(behaviorVersion1)),
          Future.successful(Some(behaviorVersion2))
        )

        when(dataService.behaviorParameters.allFor(any[BehaviorVersion])).thenReturn(Future.successful(Seq()))
        when(dataService.messageTriggers.allFor(any[BehaviorVersion])).thenReturn(Future.successful(Seq()))
        when(dataService.dataTypeConfigs.maybeFor(any[BehaviorVersion])).thenReturn(Future.successful(None))
        when(dataService.inputs.allForGroupVersion(any[BehaviorGroupVersion])).thenReturn(Future.successful(Seq()))
        when(dataService.libraries.allFor(any[BehaviorGroupVersion])).thenReturn(Future.successful(Seq()))

        when(dataService.behaviorVersions.findWithoutAccessCheck(behaviorVersion1.id)).thenReturn(Future.successful(Some(behaviorVersion1)))
        when(dataService.behaviorVersions.findWithoutAccessCheck(behaviorVersion2.id)).thenReturn(Future.successful(Some(behaviorVersion2)))

        when(dataService.behaviorVersions.maybeFunctionFor(any[BehaviorVersion])).thenReturn(Future.successful(Some(
          """function(ellipsis) {
            |
            |}"""
        )))

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

        val uuid = IDs.next
        val temp = FileUtils.getTempDirectory
        val specParentDir = new File(temp, s"githubpusherspec-$uuid")
        val parentPath = specParentDir.getAbsolutePath
        val origin = new File(specParentDir, "repo")
        FileUtils.forceMkdir(origin)

        val maybeExporter = await(BehaviorGroupExporter.maybeFor(group.id, user, dataService, cacheService, Some(parentPath), Some("repo")))
        maybeExporter.get.writeFiles()

        val git: Git = Git.init().
          setDirectory(origin).
          setGitDir(new File(origin, ".git")).
          call()
        git.add().addFilepattern(".").call()
        git.commit().setAuthor("Test", "test@test.test").
          setMessage("Initial commit").call()

        val branchName = "test_branch"

        val actionsDir = new File(origin, "actions")

        val beforeActionsDirList = listDirsIn(actionsDir)
        beforeActionsDirList mustBe Seq(behaviorVersion1Name)

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

        git.checkout().setName(branchName).call()

        val afterActionsDirList = listDirsIn(actionsDir)
        afterActionsDirList mustBe Seq(behaviorVersion2Name)

        FileUtils.deleteDirectory(specParentDir)
      }
    }
  }
}
