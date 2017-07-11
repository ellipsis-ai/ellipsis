package models

import java.nio.ByteBuffer
import java.util.UUID

object IDs {

  def uuidToBase64(uuid: UUID): String = {
    val bb = ByteBuffer.wrap(new Array[Byte](16))
    bb.putLong(uuid.getMostSignificantBits)
    bb.putLong(uuid.getLeastSignificantBits)
    java.util.Base64.getUrlEncoder.withoutPadding.encodeToString(bb.array())
  }

  def next: String = uuidToBase64(UUID.randomUUID())

}
