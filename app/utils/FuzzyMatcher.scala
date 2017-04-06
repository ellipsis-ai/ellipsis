package utils

import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric

case class FuzzyMatcher[T <: FuzzyMatchable](
                                              matchString: String,
                                              matchables: Seq[T],
                                              thresholdDelta: Double = 0.1,
                                              absoluteThreshold: Double = 0.5
                                            ) {

  val matchTokenCount: Int = matchString.split("\\s+").length

  def ngramsFor(pattern: String): Seq[String] = {
    pattern.split("\\s+").sliding(matchTokenCount, 1).map(tokens => tokens.mkString(" ")).toSeq
  }

  def basicScoreFor(text: String): Double = {
    val scores = text.sliding(matchString.length, 1).flatMap { ea =>
      RatcliffObershelpMetric.compare(ea.toLowerCase, matchString.toLowerCase)
    }
    if (scores.isEmpty) { 0 } else { scores.max }
  }

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
    if (allResults.isEmpty) {
      Seq()
    } else {
      val relativeThreshold = allResults.maxBy(_.maxScore).maxScore - thresholdDelta
      val threshold = Array(relativeThreshold, absoluteThreshold).max
      allResults.
        filter { ea => ea.maxScore > threshold }.
        map(_.filteredForThreshold(threshold)).
        sortBy(_.maxScore).
        reverse
    }
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
