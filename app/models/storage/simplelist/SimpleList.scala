package models.storage.simplelist

import models.team.Team
import org.joda.time.LocalDateTime

case class SimpleList(
                 id: String,
                 team: Team,
                 name: String,
                 createdAt: LocalDateTime
               )
