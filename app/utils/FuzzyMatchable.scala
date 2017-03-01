package utils

trait FuzzyMatchable {

  val maybeFuzzyMatchPattern: Option[String]

  val maybeText: Option[String]

}
