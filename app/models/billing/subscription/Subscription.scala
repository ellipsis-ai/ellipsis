package models.billing.subscription

import java.time.OffsetDateTime


case class Subscription(
                         id: String,
                         chargebeePlanId: String,
                         accountId: String,
                         teamId: String,
                         seatCount: Int,
                         status: String,
                         status_updated_at: OffsetDateTime,
                         created_at: OffsetDateTime
                       )
