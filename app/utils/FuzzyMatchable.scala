package utils

trait FuzzyMatchable {

  val maybeFuzzyMatchPattern: Option[String]

  val text: String

}
