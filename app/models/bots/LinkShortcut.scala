package models.bots

import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global


case class LinkShortcut(label: String, link: String, teamId: String) {
  def save: DBIO[LinkShortcut] = LinkShortcut.save(this)
}

class LinkShortcutsTable(tag: Tag) extends Table[LinkShortcut](tag, "link_shortcuts") {

  def label = column[String]("label")
  def link = column[String]("link")
  def teamId = column[String]("team_id")

  def * = (label, link, teamId) <> ((LinkShortcut.apply _).tupled, LinkShortcut.unapply _)
}

object LinkShortcut {

  val all = TableQuery[LinkShortcutsTable]

  def uncompiledFindQueryFor(label: Rep[String], teamId: Rep[String]) = {
    all.filter(_.label === label)
  }
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def find(label: String, teamId: String): DBIO[Option[LinkShortcut]] = {
    findQueryFor(label, teamId).result.map(_.headOption)
  }

  def save(shortcut: LinkShortcut): DBIO[LinkShortcut] = {
    val query = findQueryFor(shortcut.label, shortcut.teamId)
    query.result.flatMap { result =>
      result.headOption.map { existing =>
        all.filter(_.label === shortcut.label).update(shortcut)
      }.getOrElse {
        all += shortcut
      }.map { _ => shortcut }
    }
  }
}
