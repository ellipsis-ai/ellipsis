package models.data.apibackeddatatype

import models.team.Team
import org.joda.time.DateTime

case class ApiBackedDataType(
                              id: String,
                              team: Team,
                              maybeCurrentVersionId: Option[String],
                              maybeImportedId: Option[String],
                              createdAt: DateTime
                            )
