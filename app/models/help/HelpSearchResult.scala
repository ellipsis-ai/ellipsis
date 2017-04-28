package models.help

import json.BehaviorTriggerData
import models.behaviors.events.Event
import services.{AWSLambdaService, DataService}
import utils.FuzzyMatchResult

import scala.util.matching.Regex

case class HelpSearchResult(
                             searchQuery: String,
                             underlying: FuzzyMatchResult[HelpGroupData],
                             event: Event,
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
      searchPattern.replaceAllIn(group.description, "$1**$2**$3")
    } else {
      group.description
    }
  }

}
