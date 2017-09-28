import json.SlackUserData
import models.behaviors.events.SlackMessage
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsObject

class SlackMessageSpec extends PlaySpec {

  val userId = "U1234567"
  val user = SlackUserData(userId, "T1234", "attaboy", Some("Luke Andrews"), Some("America/Toronto"), JsObject(Seq()))
  val userList = Set(user)

  "unformatLinks" should {
    "unformat channels and users" in {
      SlackMessage.unformatLinks("<@U12345678>") mustBe "@U12345678"
      SlackMessage.unformatLinks("<@U12345678|attaboy>") mustBe "@attaboy"
      SlackMessage.unformatLinks("<@W12345678>") mustBe "@W12345678"
      SlackMessage.unformatLinks("<@W12345678|enterprise>") mustBe "@enterprise"
      SlackMessage.unformatLinks("<#C12345789>") mustBe "#C12345789"
      SlackMessage.unformatLinks("<#C12345789|general>") mustBe "#general"
    }

    "unformat special commands" in {
      SlackMessage.unformatLinks("<!here>") mustBe "@here"
      SlackMessage.unformatLinks("<!here|here>") mustBe "@here"
      SlackMessage.unformatLinks("<!group>") mustBe "@group"
      SlackMessage.unformatLinks("<!group|group>") mustBe "@group"
      SlackMessage.unformatLinks("<!channel>") mustBe "@channel"
      SlackMessage.unformatLinks("<!channel|channel>") mustBe "@channel"
      SlackMessage.unformatLinks("<!everyone>") mustBe "@everyone"
      SlackMessage.unformatLinks("<!everyone|everyone>") mustBe "@everyone"
      SlackMessage.unformatLinks("<!subteam^1234|foo>") mustBe "@foo"
      SlackMessage.unformatLinks("<!date^1392734382^{date} at {time}|February 18th, 2014 at 6:39 AM PST>") mustBe "February 18th, 2014 at 6:39 AM PST"
      SlackMessage.unformatLinks("<!unrecognized|foo>") mustBe "<foo>"
      SlackMessage.unformatLinks("<!unrecognized>") mustBe "<unrecognized>"
    }

    "replace email and web links with the original link text" in {
      SlackMessage.unformatLinks("<mailto:luke@ellipsis.ai|luke@ellipsis.ai>") mustBe "luke@ellipsis.ai"
      SlackMessage.unformatLinks("<mailto:luke@ellipsis.ai>") mustBe "mailto:luke@ellipsis.ai"
      SlackMessage.unformatLinks("<https://bot.ellipsis.ai/|https://bot.ellipsis.ai/>") mustBe "https://bot.ellipsis.ai/"
      SlackMessage.unformatLinks("<https://bot.ellipsis.ai/>") mustBe "https://bot.ellipsis.ai/"
    }

    "not unformat incomplete links" in {
      SlackMessage.unformatLinks("<@U12345678") mustBe "<@U12345678"
    }
  }

  "unescapeSlackHTMLEntities" should {

    "unscape HTML entities" in {
      SlackMessage.unescapeSlackHTMLEntities("&amp;") mustBe "&"
      SlackMessage.unescapeSlackHTMLEntities("&amp;quot;") mustBe "&quot;"
      SlackMessage.unescapeSlackHTMLEntities("&lt;&amp;amp;&gt;") mustBe "<&amp;>"
    }

  }

  "augmentUserIdsWithNames" should {
    "add user names to user links" in {
      SlackMessage.augmentUserIdsWithNames(s"Hey <@${user.accountId}>, what's shaking?", userList) mustBe s"Hey <@${user.accountId}|${user.accountName}>, what's shaking?"
    }
  }

  "unformatText" should {

    "strip links first, and unescape HTML entities second" in {
      SlackMessage.unformatTextWithUsers("&lt;https://bot.ellipsis.ai/?foo&amp;bar&gt;", userList) mustBe "<https://bot.ellipsis.ai/?foo&bar>"
    }

    "handle a complex message" in {
      val received =
        s"""Hey <!channel|channel>, has anyone seen <mailto:luke@ellipsis.ai|luke@ellipsis.ai>?
           |
           |He &amp; I have a meeting. <@${user.accountId}>, have you seen him?""".stripMargin
      val expected =
        s"""Hey @channel, has anyone seen luke@ellipsis.ai?
           |
           |He & I have a meeting. @${user.accountName}, have you seen him?""".stripMargin
      SlackMessage.unformatTextWithUsers(received, userList) mustBe expected
    }

  }

  "userIdsInText" should {
    "return the user IDs as a set" in {
      SlackMessage.userIdsInText("Hi there, <@U12345678>.") mustBe Set("U12345678")
      SlackMessage.userIdsInText("<@W12345678> is a real piece of work.") mustBe Set("W12345678")
      SlackMessage.userIdsInText("<@W12345678> is a real piece of work. Oh that <@W12345678>") mustBe Set("W12345678")
    }
  }

}
