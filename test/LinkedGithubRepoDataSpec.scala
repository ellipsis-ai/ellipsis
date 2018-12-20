import json.{BehaviorGroupData, LinkedGithubRepoData}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import support.TestContext

class LinkedGithubRepoDataSpec extends PlaySpec with MockitoSugar {
  "maybeFrom(BehaviorGroupData)" should {
    "maintain the LinkedGithubRepoData if any" in new TestContext {
      val groupData = mock[BehaviorGroupData]
      when(groupData.linkedGithubRepo).thenReturn(Some(LinkedGithubRepoData("theOwner", "theRepo", Some("theBranch"))))
      val linkedGithubRepoData = LinkedGithubRepoData.maybeFrom(groupData).get
      linkedGithubRepoData.owner mustBe "theOwner"
      linkedGithubRepoData.repo mustBe "theRepo"
      linkedGithubRepoData.currentBranch mustBe "theBranch"
    }
  }
}
