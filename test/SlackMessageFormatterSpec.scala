import models.SlackMessageFormatter
import org.scalatestplus.play.PlaySpec

class SlackMessageFormatterSpec extends PlaySpec {

  def format(original: String): String = {
    SlackMessageFormatter.bodyTextFor(original).trim
  }

  "bodyTextFor" should {

    "handle blockquotes" in {
      format(">foo bar") mustBe "> foo bar"
      format(">_foo_ bar") mustBe "> _foo_ bar"
      format(">foo\n>bar\n>\n>baz") mustBe "> foo\r> bar\r> \r> baz"
      format(">_foo_\n>bar\n>\n>baz") mustBe "> _foo_\r> bar\r> \r> baz"
    }

    "preserve soft linebreaks" in {
      format("my balogna has a first name\nit's O-S-C-A-R") mustBe "my balogna has a first name\rit's O-S-C-A-R"
    }

    "insert soft hyphens around dangling special characters that aren't used to format" in {
      format("__this is `bold with~ special char_acters*__") mustBe "*this is \u00AD`\u00ADbold with\u00AD~\u00AD special char_acters\u00AD*\u00AD*"
    }

  }

}
