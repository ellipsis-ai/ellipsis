import models.IDs
import models.behaviors.nodemoduleversion.NodeModuleVersion
import org.scalatestplus.play.PlaySpec

class NodeModuleVersionSpec extends PlaySpec {
  "nameWithoutVersion" should {
    "leave a regular package name alone" in {
      val v = NodeModuleVersion(IDs.next, "somepackagename", "1.0.0", IDs.next)
      v.nameWithoutVersion mustBe "somepackagename"
    }

    "strip a @[version number] from the name" in {
      val v = NodeModuleVersion(IDs.next, "somepackagename@1.0.0", "1.0.0", IDs.next)
      v.nameWithoutVersion mustBe "somepackagename"
    }

    "leave the @ in scoped package names" in {
      val v = NodeModuleVersion(IDs.next, "@somescope/somepackagename", "1.0.0", IDs.next)
      v.nameWithoutVersion mustBe "@somescope/somepackagename"
    }

    "strip a @[version number] but leave the scoped package name" in {
      val v = NodeModuleVersion(IDs.next, "@somescope/somepackagename@1.0.0", "1.0.0", IDs.next)
      v.nameWithoutVersion mustBe "@somescope/somepackagename"
    }
  }
}
