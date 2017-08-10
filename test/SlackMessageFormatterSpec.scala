import models.SlackMessageFormatter
import org.scalatestplus.play.PlaySpec
import org.apache.commons.lang.StringEscapeUtils.escapeJava

class SlackMessageFormatterSpec extends PlaySpec {

  // Compare escaped strings instead of actual strings because the test runner chokes on strings with new lines

  def format(original: String): String = {
    escapeJava(SlackMessageFormatter.bodyTextFor(original))
  }

  "bodyTextFor" should {

    "handle blockquotes" in {
      format(">foo bar") mustBe """\r> foo bar\r> """
      format(">_foo_ bar") mustBe """\r> _foo_ bar\r> """
      format(">foo\n>bar\n>\n>baz") mustBe """\r> foo bar\r> \r> baz\r> """
      format(">_foo_\n>bar\n>\n>baz") mustBe """\r> _foo_ bar\r> \r> baz\r> """
    }

  }

}
