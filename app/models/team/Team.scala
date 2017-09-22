package models.team

import java.time.{OffsetDateTime, ZoneId}

case class Team(
                 id: String,
                 name: String,
                 maybeTimeZone: Option[ZoneId],
                 createdAt: OffsetDateTime
               ) {

  def maybeNonEmptyName: Option[String] = Option(name).filter(_.trim.nonEmpty)

  def timeZone: ZoneId = maybeTimeZone.getOrElse(ZoneId.systemDefault)

}
