package models.team

import java.time.ZoneId

case class Team(
                 id: String,
                 name: String,
                 maybeTimeZone: Option[ZoneId]
               ) {

  def maybeNonEmptyName: Option[String] = Option(name).filter(_.trim.nonEmpty)

  def timeZone: ZoneId = maybeTimeZone.getOrElse(ZoneId.systemDefault)

}
