import java.util.UUID

import org.scalatestplus.play.PlaySpec
import models.IDs

class IDsSpec extends PlaySpec {
  val long1 = java.lang.Long.parseUnsignedLong("2025698a5ee34264", 16)
  val long2 = java.lang.Long.parseUnsignedLong("a9edc8befe6f61bd", 16)
  val uuid = new UUID(long1, long2)

  "uuidToBase64" should {
    "be consistent with the JavaScript ID.toBase64 for a particular UUID" in {
      uuid.toString mustBe "2025698a-5ee3-4264-a9ed-c8befe6f61bd"
      IDs.uuidToBase64(uuid) mustBe "ICVpil7jQmSp7ci-_m9hvQ"
    }
  }
}
