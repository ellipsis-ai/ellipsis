import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import slick.dbio.DBIO
import support.TestContext


class FooSpec extends PlaySpec {

  "Foo" should {

    "work" in new TestContext {
      running(app) {
        val success = DBIO.successful(42)
        dataService.runNow(success) mustBe 42
        assertThrows[Exception] {
          val failure = DBIO.failed(new Exception("boom"))
          dataService.runNow(failure)
        }
      }
    }

  }

}
