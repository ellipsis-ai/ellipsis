package models.organization

import models.IDs
import java.time.OffsetDateTime

case class Organization(
                         id: String,
                         name: String,
                         chargebeeCustomerId: String,
                         created_at: OffsetDateTime
                       ) {
  def this(name: String) = this(IDs.next, name, IDs.next, OffsetDateTime.now)
}

