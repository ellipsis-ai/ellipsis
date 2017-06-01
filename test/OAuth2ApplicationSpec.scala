import models.accounts.oauth2application.OAuth2Application
import models.accounts.oauth2api._
import org.scalatestplus.play.PlaySpec

class OAuth2ApplicationSpec extends PlaySpec {

  val api = OAuth2Api("api1", "api", OAuth2GrantType.values.head, None, "https://lololol.com/", None, None, None)
  def appNamed(name: String): OAuth2Application = {
    OAuth2Application(
      "id",
      name,
      api,
      "clientId",
      "clientSecret",
      None,
      "team",
      isShared = false
    )
  }

  "OAuth2Application.keyName" should {
    "convert a phrase into a camel-case identifier" in {
      appNamed("Todoist add item").keyName mustBe "todoistAddItem"
    }

    "strip non-valid characters and extra spaces" in {
      val app = appNamed("The quick  brown FOX — it jumped over the lazy dog!")
      app.keyName mustBe "theQuickBrownFOXItJumpedOverTheLazyDog"
    }

    "ensure the first character is valid" in {
      val app = appNamed("1 for the money")
      app.keyName mustBe "_1ForTheMoney"
    }
  }
}
