package models.organization

import java.time.OffsetDateTime

case class Organization(
                         id: String,
                         name: String,
                         chargebeeCustomerId: String,
                         created_at: OffsetDateTime
                       )

