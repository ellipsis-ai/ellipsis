package models.help

import json.BehaviorGroupData
import services.{AWSLambdaService, DataService}

case class SimpleHelpResult(
                             group: BehaviorGroupData,
                             dataService: DataService,
                             lambdaService: AWSLambdaService
                           ) extends HelpResult {
  val matchingTriggers = Seq()

  def description: String = trimmedGroupDescription
}
