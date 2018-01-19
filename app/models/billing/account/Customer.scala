package models.billing.account

import java.time.OffsetDateTime

case class Customer(id: String, chargebeeId: String, created_at: OffsetDateTime)

