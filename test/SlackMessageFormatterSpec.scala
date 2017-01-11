import models.SlackMessageFormatter
import org.scalatestplus.play.PlaySpec
import org.apache.commons.lang.StringEscapeUtils.escapeJava

class SlackMessageFormatterSpec extends PlaySpec {

  "SlackMessageFormatter" should {

    "handle blockquotes" in {
      escapeJava(SlackMessageFormatter.bodyTextFor(">foo")) mustBe """\r> foo \r> """
      escapeJava(SlackMessageFormatter.bodyTextFor(">foo\n>bar\n>\n>baz")) mustBe """\r> foo bar \r> \r> baz \r> """
      escapeJava(SlackMessageFormatter.bodyTextFor(">_foo_ bar\n>\n>baz")) mustBe """\r> _foo_ bar \r> \r> baz \r> """
    }

  }

}
