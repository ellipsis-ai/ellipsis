package models.help

case class HelpGroupSearchValue(helpGroupId: String, maybeSearchText: Option[String]) {
  override def toString: String = {
    maybeSearchText.map { searchText =>
      s"id=$helpGroupId&search=$searchText"
    }.getOrElse(helpGroupId)
  }
}

object HelpGroupSearchValue {
  val idAndSearchPattern = "id=(.+?)&search=(.+)".r

  def fromString(string: String): HelpGroupSearchValue = {
    string match {
      case idAndSearchPattern(id, search) => HelpGroupSearchValue(id, Some(search))
      case value => HelpGroupSearchValue(value, None)
    }
  }
}
