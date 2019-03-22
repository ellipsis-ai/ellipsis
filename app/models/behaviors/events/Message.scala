package models.behaviors.events

trait Message {
  val maybeId: Option[String]
  val maybeThreadId: Option[String]
}
