import java.time.OffsetDateTime

import models.IDs
import models.apitoken.APIToken
import org.scalatestplus.play.PlaySpec

class APITokenSpec extends PlaySpec {

  def newToken(
                maybeExpirySeconds: Option[Int],
                isOneTime: Boolean,
                isRevoked: Boolean,
                createdAt: OffsetDateTime = OffsetDateTime.now
              ): APIToken = {
    APIToken(IDs.next, IDs.next, IDs.next, maybeExpirySeconds, isOneTime, isRevoked, Some(createdAt), createdAt)
  }

  "isValid" should {

    "be true when no expiry, not one-time and not revoked" in {
      val token = newToken(None, isOneTime = false, isRevoked = false)
      token.isValid mustBe true
    }

    "be false when revoked" in {
      val token = newToken(None, isOneTime = false, isRevoked = true)
      token.isValid mustBe false
    }

    "be false when expired" in {
      val expirySeconds = 300
      val token = newToken(Some(expirySeconds), isOneTime = false, isRevoked = false, createdAt = OffsetDateTime.now.minusSeconds(expirySeconds + 1))
      token.isValid mustBe false
    }

    "be true when expiry seconds haven't yet passed" in {
      val expirySeconds = 300
      val token = newToken(Some(expirySeconds), isOneTime = false, isRevoked = false, createdAt = OffsetDateTime.now.minusSeconds(expirySeconds - 1))
      token.isValid mustBe true
    }

    "be false when one-time and already used" in {
      val token = newToken(None, isOneTime = true, isRevoked = true)
      token.isValid mustBe false
    }

  }

}
