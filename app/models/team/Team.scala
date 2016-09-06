package models.team

case class Team(
                 id: String,
                 name: String
               ) {

  def maybeNonEmptyName: Option[String] = Option(name).filter(_.trim.nonEmpty)

}
