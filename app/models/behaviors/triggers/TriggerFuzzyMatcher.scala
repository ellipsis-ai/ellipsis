package models.behaviors.triggers

import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric

case class TriggerFuzzyMatcher[T <: FuzzyMatchable](matchString: String, triggers: Seq[T]) {

  val matchTokenCount: Int = matchString.split("\\s+").length

  def ngramsFor(pattern: String): Seq[String] = {
    pattern.split("\\s+").sliding(matchTokenCount, 1).map(tokens => tokens.mkString(" ")).toSeq
  }

  def basicScoreFor(text: String): Double = RatcliffObershelpMetric.compare(text, matchString).getOrElse(0)

  def scoreFor(trigger: T): Double = {
    trigger.maybeFuzzyMatchPattern.map { pattern =>
      ngramsFor(pattern).map(basicScoreFor).max
    }.getOrElse(0)
  }

  def run: Seq[(T, Double)] = {
    val sortedWithSimilarity =
      triggers.
        map { ea => (ea, scoreFor(ea)) }.
        sortBy { case(_, similarity) => similarity }.
        reverse

    sortedWithSimilarity.headOption.map(_._2 - 0.1).map { threshold =>
      sortedWithSimilarity.filter { case(_, similarity) => similarity > threshold }
    }.getOrElse(Seq())
  }

  def hasAnyMatches: Boolean = {
    run.nonEmpty
  }

}
