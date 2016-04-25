package models.bots

trait Trigger {
  val id: String
  val behavior: Behavior

  def isActivatedBy(event: Event): Boolean
}
