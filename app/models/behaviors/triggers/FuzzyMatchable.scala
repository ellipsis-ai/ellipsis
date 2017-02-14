package models.behaviors.triggers

trait FuzzyMatchable {

  val maybeFuzzyMatchPattern: Option[String]

  val text: String

}
