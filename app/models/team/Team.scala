package models.team

import org.joda.time.DateTimeZone

case class Team(
                 id: String,
                 name: String,
                 maybeTimeZone: Option[DateTimeZone]
               ) {

  def maybeNonEmptyName: Option[String] = Option(name).filter(_.trim.nonEmpty)

  def timeZone: DateTimeZone = maybeTimeZone.getOrElse(DateTimeZone.getDefault)

}
