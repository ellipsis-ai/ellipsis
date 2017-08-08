import models.SlackMessageFormatter
import models.accounts.slack.SlackUserInfo
import org.scalatestplus.play.PlaySpec
import org.apache.commons.lang.StringEscapeUtils.escapeJava

class SlackMessageFormatterSpec extends PlaySpec {

  // Compare escaped strings instead of actual strings because the test runner chokes on strings with new lines

  def format(original: String): String = {
    escapeJava(SlackMessageFormatter.bodyTextFor(original))
  }

  val userId = "U1234567"
  val user = SlackUserInfo(userId, "attaboy", Some("America/Toronto"))
  val userList = Seq(user)

  "bodyTextFor" should {

    "handle blockquotes" in {
      format(">foo bar") mustBe """\r> foo bar\r> """
      format(">_foo_ bar") mustBe """\r> _foo_ bar\r> """
      format(">foo\n>bar\n>\n>baz") mustBe """\r> foo bar\r> \r> baz\r> """
      format(">_foo_\n>bar\n>\n>baz") mustBe """\r> _foo_ bar\r> \r> baz\r> """
    }

  }

  "unformatLinks" should {
    "unformat channels and users" in {
      SlackMessageFormatter.unformatLinks("<@U12345678>") mustBe "@U12345678"
      SlackMessageFormatter.unformatLinks("<@U12345678|attaboy>") mustBe "@attaboy"
      SlackMessageFormatter.unformatLinks("<@W12345678>") mustBe "@U12345678"
      SlackMessageFormatter.unformatLinks("<@W12345678|enterprise") mustBe "@enterprise"
      SlackMessageFormatter.unformatLinks("<#C12345789>") mustBe "#C12345789"
      SlackMessageFormatter.unformatLinks("<#C12345789|general>") mustBe "#general"
    }

    "unformat special commands" in {
      SlackMessageFormatter.unformatLinks("<!here>") mustBe "@here"
      SlackMessageFormatter.unformatLinks("<!here|here>") mustBe "@here"
      SlackMessageFormatter.unformatLinks("<!group>") mustBe "@group"
      SlackMessageFormatter.unformatLinks("<!group|group>") mustBe "@group"
      SlackMessageFormatter.unformatLinks("<!channel>") mustBe "@channel"
      SlackMessageFormatter.unformatLinks("<!channel|channel>") mustBe "@channel"
      SlackMessageFormatter.unformatLinks("<!everyone>") mustBe "@everyone"
      SlackMessageFormatter.unformatLinks("<!everyone|everyone>") mustBe "@everyone"
      SlackMessageFormatter.unformatLinks("<!subteam^1234|foo>") mustBe "@foo"
      SlackMessageFormatter.unformatLinks("<!date^1392734382^{date} at {time}|February 18th, 2014 at 6:39 AM PST>") mustBe "February 18th, 2014 at 6:39 AM PST"
      SlackMessageFormatter.unformatLinks("<!unrecognized|foo>") mustBe "<foo>"
      SlackMessageFormatter.unformatLinks("<!unrecognized>") mustBe "<unrecognized>"
    }

    "replace email and web links with the original link text" in {
      SlackMessageFormatter.unformatLinks("<mailto:luke@ellipsis.ai|luke@ellipsis.ai>") mustBe "luke@ellipsis.ai"
      SlackMessageFormatter.unformatLinks("<mailto:luke@ellipsis.ai>") mustBe "mailto:luke@ellipsis.ai"
      SlackMessageFormatter.unformatLinks("<https://bot.ellipsis.ai/|https://bot.ellipsis.ai/>") mustBe "https://bot.ellipsis.ai/"
      SlackMessageFormatter.unformatLinks("<https://bot.ellipsis.ai/>") mustBe "https://bot.ellipsis.ai/"
    }
  }

  "unescapeSlackHTMLEntities" should {

    "unscape HTML entities" in {
      SlackMessageFormatter.unescapeSlackHTMLEntities("&amp;") mustBe "&"
      SlackMessageFormatter.unescapeSlackHTMLEntities("&amp;quot;") mustBe "&quot;"
      SlackMessageFormatter.unescapeSlackHTMLEntities("&lt;&amp;amp;&gt;") mustBe "<&amp;>"
    }

  }

  "augmentUserIdsWithNames" should {
    "add user names to user links" in {
      SlackMessageFormatter.augmentUserIdsWithNames(s"Hey <@${user.userId}>, what's shaking?", userList) mustBe s"Hey <@${user.userId}|${user.name}>, what's shaking?"
    }
  }

  "unformatText" should {

    "strip links first, and unescape HTML entities second" in {
      SlackMessageFormatter.unformatText("&lt;https://bot.ellipsis.ai/?foo&amp;bar&gt;", userList) mustBe "<https://bot.ellipsis.ai/?foo&bar>"
    }

    "handle a complex message" in {
      val received =
        s"""Hey <!channel|channel>, has anyone seen <mailto:luke@ellipsis.ai|luke@ellipsis.ai>?
          |
          |He &amp; I have a meeting. <@${user.userId}>, have you seen him?""".stripMargin
      val expected =
        s"""Hey @channel, has anyone seen luke@ellipsis.ai?
          |
          |He & I have a meeting. @${user.name}, have you seen him?""".stripMargin
      SlackMessageFormatter.unformatText(received, userList) mustBe expected
    }

  }

}
