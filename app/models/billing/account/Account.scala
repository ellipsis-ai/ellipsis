package models.billing.account

import java.time.OffsetDateTime

case class Account(id: String, chargebeeId: String, created_at: OffsetDateTime)

