package utils

import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric

case class FuzzyMatcher[T <: FuzzyMatchable](
                                              matchString: String,
                                              matchables: Seq[T],
                                              thresholdDelta: Double = 0.1
                                            ) {

  val matchTokenCount: Int = matchString.split("\\s+").length

  def ngramsFor(pattern: String): Seq[String] = {
    pattern.split("\\s+").sliding(matchTokenCount, 1).map(tokens => tokens.mkString(" ")).toSeq
  }

  def basicScoreFor(text: String): Double = RatcliffObershelpMetric.compare(text.toLowerCase, matchString.toLowerCase).getOrElse(0)

  def scoreFor(matchable: T): Double = {
    val patterns = matchable.fuzzyMatchPatterns.flatMap(_.maybePattern)
    val scores = patterns.map { pattern =>
      ngramsFor(pattern.toLowerCase).map(basicScoreFor).max
    }
    scores.sorted.reverse.headOption.getOrElse(0)
  }

  def resultFor(matchable: T): FuzzyMatchResult[T] = {
    FuzzyMatchResult(matchable, matchable.fuzzyMatchPatterns.flatMap { ea =>
      ea.maybePattern.map { pattern =>
        (ea, ngramsFor(pattern.toLowerCase).map(basicScoreFor).max)
      }
    })
  }

  def allResults: Seq[FuzzyMatchResult[T]] = {
    matchables.map(resultFor)
  }

  def run: Seq[FuzzyMatchResult[T]] = {
    allResults.sortBy(_.maxScore).reverse
    val sortedWithSimilarity = allResults.sortBy(_.maxScore).reverse

    sortedWithSimilarity.headOption.map(_.maxScore - thresholdDelta).map { threshold =>
      sortedWithSimilarity.
        filter { ea => ea.maxScore > threshold }.
        map(_.filteredForThreshold(threshold))
    }.getOrElse(Seq())
  }

  def hasAnyMatches: Boolean = {
    run.nonEmpty
  }

}

case class FuzzyMatchResult[T <: FuzzyMatchable](item: T, patternsWithScores: Seq[(FuzzyMatchPattern, Double)]) {
  val maxScore: Double = if (patternsWithScores.isEmpty) { 0 } else { patternsWithScores.map(_._2).max }
  val patterns: Seq[FuzzyMatchPattern] = patternsWithScores.map(_._1)
  def filteredForThreshold(threshold: Double): FuzzyMatchResult[T] = {
    copy(patternsWithScores = patternsWithScores.filter { case(pattern, score) =>
      score > threshold
    })
  }
}
