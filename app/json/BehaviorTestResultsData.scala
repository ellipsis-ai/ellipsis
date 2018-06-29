package json

import models.behaviors.behaviortestresult.BehaviorTestResult

case class BehaviorTestResultsData(results: Seq[BehaviorTestResult], shouldRetry: Boolean)

