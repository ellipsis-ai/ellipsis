package json

import java.time.OffsetDateTime

case class AdminTeamData(
                          id: String,
                          name: String,
                          timeZone: String,
                          createdAt: OffsetDateTime,
                          allowShortcutMention: Boolean
                        )
