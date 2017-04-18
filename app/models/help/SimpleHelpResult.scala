package models.help

import models.behaviors.events.Event
import services.{AWSLambdaService, DataService}

case class SimpleHelpResult(
                             group: HelpGroupData,
                             event: Event,
                             dataService: DataService,
                             lambdaService: AWSLambdaService
                           ) extends HelpResult {
  val matchingTriggers = Seq()

  def description: String = trimmedGroupDescription
}
