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

    "escape any lingering < > & characters in non-formatted text except slack user/channel links" in {
      format("__1 is < than 2 & 4 > 3__") mustBe "*1 is &lt; than 2 &amp; 4 &gt; 3*"
      format("[This is a <special> link](http://special.com)") mustBe "<http://special.com|This is a &lt;special&gt; link>"
      format("[This is a \\<special\\> link](http://special.com)") mustBe "<http://special.com|This is a &lt;special&gt; link>"
      format("1 < 2 but this is a <@U1234> link to a “<#channel>”!") mustBe "1 &lt; 2 but this is a <@U1234> link to a “<#channel>”!"
    }

    "handle formatting inside links" in {
      format("[**This is a bold link**](http://ohsobold.com)") mustBe "<http://ohsobold.com|*This is a bold link*>"
    }

    "handle regex chars" in {
      format("The $latest is the <latest>") mustBe "The $latest is the &lt;latest&gt;"
    }

    "handle horizontal rule" in {
      val input = """There should be a line below this
                    |
                    |---
                    |
                    |There should be a line above this""".stripMargin
      val output = "There should be a line below this\r\r─────\r\rThere should be a line above this"
      format(input) mustBe output
    }

  }

}
