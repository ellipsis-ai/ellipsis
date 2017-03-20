package models.help

import json.{BehaviorGroupData, BehaviorTriggerData}
import services.{AWSLambdaService, DataService}
import utils.FuzzyMatchResult

import scala.util.matching.Regex

case class HelpSearchResult(
                             searchQuery: String,
                             underlying: FuzzyMatchResult[BehaviorGroupData],
                             dataService: DataService,
                             lambdaService: AWSLambdaService
                           ) extends HelpResult {
  val group = underlying.item
  val descriptionMatches: Boolean = underlying.patterns.contains(group.fuzzyMatchDescription)
  val matchingTriggers = underlying.patterns.flatMap {
    case trigger: BehaviorTriggerData => Some(trigger)
    case _ => None
  }

  private def searchPattern: Regex = {
    s"(?i)(\\s|\\A)(\\S*${Regex.quote(searchQuery)}\\S*)(\\s|\\Z)".r
  }

  def description: String = {
    if (descriptionMatches) {
      searchPattern.replaceAllIn(trimmedGroupDescription, "$1**$2**$3")
    } else {
      trimmedGroupDescription
    }
  }

}
