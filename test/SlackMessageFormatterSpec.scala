import models.SlackMessageFormatter
import org.scalatestplus.play.PlaySpec

class SlackMessageFormatterSpec extends PlaySpec {

  "SlackMessageFormatter" should {

    "handle blockquotes" in  {
      SlackMessageFormatter.bodyTextFor(">foo") mustBe "\r> foo"
    }

  }

}
