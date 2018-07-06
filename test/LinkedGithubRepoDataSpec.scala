import json.{BehaviorGroupData, LinkedGithubRepoData}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import support.TestContext

class LinkedGithubRepoDataSpec extends PlaySpec with MockitoSugar {
  "maybeFrom(BehaviorGroupData)" should {
    "maintain the LinkedGithubRepoData if any" in new TestContext {
      val groupData = mock[BehaviorGroupData]
      when(groupData.githubUrl).thenReturn(Some("https://github.com/wrong/repo"))
      when(groupData.linkedGithubRepo).thenReturn(Some(LinkedGithubRepoData("theOwner", "theRepo", Some("theBranch"))))
      val linkedGithubRepoData = LinkedGithubRepoData.maybeFrom(groupData).get
      linkedGithubRepoData.owner mustBe "theOwner"
      linkedGithubRepoData.repo mustBe "theRepo"
      linkedGithubRepoData.currentBranch mustBe "theBranch"
    }

    "fallback to the github URL if no LinkedGithubRepoData" in new TestContext {
      val groupData = mock[BehaviorGroupData]
      when(groupData.githubUrl).thenReturn(Some("https://github.com/theOwner/theRepo"))
      when(groupData.linkedGithubRepo).thenReturn(None)
      val linkedGithubRepoData = LinkedGithubRepoData.maybeFrom(groupData).get
      linkedGithubRepoData.owner mustBe "theOwner"
      linkedGithubRepoData.repo mustBe "theRepo"
      linkedGithubRepoData.currentBranch mustBe "master"
    }
  }
}
