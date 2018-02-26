package models.team

import models.IDs
import java.time.{OffsetDateTime, ZoneId}

case class Team(
                 id: String,
                 name: String,
                 maybeTimeZone: Option[ZoneId],
                 maybeOrganizationId: Option[String],
                 createdAt: OffsetDateTime
               ) {

  def maybeNonEmptyName: Option[String] = Option(name).filter(_.trim.nonEmpty)

  def timeZone: ZoneId = maybeTimeZone.getOrElse(ZoneId.systemDefault)

}

object Team {

  def apply(name: String): Team = Team(
    IDs.next,
    name,
    None,
    None,
    OffsetDateTime.now
  )

  def defaultBotName: String = "YourCompanyBot"

}
