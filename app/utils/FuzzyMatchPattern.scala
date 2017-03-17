package utils

trait FuzzyMatchPattern {

  val maybePattern: Option[String]

}

case class SimpleFuzzyMatchPattern(maybePattern: Option[String]) extends FuzzyMatchPattern
