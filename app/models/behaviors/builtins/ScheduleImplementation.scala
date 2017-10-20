package models.behaviors.builtins

import akka.actor.ActorSystem
import json._
import models.behaviors.events.Event
import models.behaviors.{BotResult, ParameterWithValue, SimpleTextResult}
import models.team.Team
import play.api.libs.json.JsBoolean
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}


case class ScheduleImplementation(
                             text: String,
                             isForIndividualMembers: Boolean,
                             recurrence: String,
                             event: Event,
                             services: DefaultServices
                           ) extends BuiltinImplementation {

  def result(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    val dataService = services.dataService
    for {
      user <- event.ensureUser(dataService)
      maybeTeam <- dataService.teams.find(user.teamId)
      maybeScheduledMessage <- maybeTeam.map { team =>
        dataService.scheduledMessages.maybeCreateWithRecurrenceText(text, recurrence, user, team, event.maybeChannel, isForIndividualMembers)
      }.getOrElse(Future.successful(None))
      responseText <- maybeScheduledMessage.map { scheduledMessage =>
        scheduledMessage.successResponse(dataService)
      }.getOrElse(Future.successful(s"Sorry, I don’t know how to schedule `$recurrence`"))
    } yield {
      SimpleTextResult(event, None, responseText, forcePrivateResponse = false)
    }
  }

}

object ScheduleImplementation extends BuiltinImplementationType {

  val builtinId: String = "schedule"
  val messageInputId: String = "message-input-id"
  val messageInputName: String = "message"
  val recurrenceInputId: String = "recurrence-input-id"
  val recurrenceInputName: String = "recurrence"
  val isForIndividualMembersInputId: String = "is-for-individual-members"
  val isForIndividualMembersInputName: String = "isForIndividualMembers"

  def triggersData: Seq[BehaviorTriggerData] = {
    Seq(
      BehaviorTriggerData("""^\s*schedule\s*$$""", requiresMention = true, isRegex = true, caseSensitive = false),
      BehaviorTriggerData(s"""^schedule\\s+([`"'])(.*?)\\1(\\s+privately for everyone in this channel)?\\s+(.*)\\s*$$""", requiresMention = true, isRegex = true, caseSensitive = false)
    )
  }

  def inputsData: Seq[InputData] = {
    Seq(
      InputData(
        None,
        Some(messageInputId),
        None,
        messageInputName,
        Some(BehaviorParameterTypeData.text),
        "What do you want to schedule?",
        isSavedForTeam = false,
        isSavedForUser = false
      ),
      InputData(
        None,
        Some(recurrenceInputId),
        None,
        recurrenceInputName,
        Some(BehaviorParameterTypeData.text),
        "When do you want this to run?",
        isSavedForTeam = false,
        isSavedForUser = false
      ),
      InputData(
        None,
        Some(isForIndividualMembersInputId),
        None,
        isForIndividualMembersInputName,
        Some(BehaviorParameterTypeData.yesNo),
        "Do you want it to run privately for each member of this channel?",
        isSavedForTeam = false,
        isSavedForUser = false
      )
    )
  }

  def behaviorVersionsDataFor(team: Team, dataService: DataService): Seq[BehaviorVersionData] = {
    Seq(
      BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, Some("Schedule an action"), dataService).copy(
        triggers = triggersData,
        inputIds = Seq(messageInputId, recurrenceInputId, isForIndividualMembersInputId),
        builtinName = Some(builtinId)
      )
    )
  }

  def addToGroupData(data: BehaviorGroupData, team: Team, dataService: DataService): BehaviorGroupData = {
    data.copy(
      actionInputs = data.actionInputs ++ inputsData,
      behaviorVersions = data.behaviorVersions ++ behaviorVersionsDataFor(team, dataService)
    )
  }

  def maybeMessageTextFor(parametersWithValues: Seq[ParameterWithValue]): Option[String] = {
    parametersWithValues.
      find(_.parameter.input.name == ScheduleImplementation.messageInputName).
      flatMap(_.maybeValue.map(_.text))
  }

  def isForIndividualMembersFor(parametersWithValues: Seq[ParameterWithValue]): Option[Boolean] = {
    parametersWithValues.
      find(_.parameter.input.name == ScheduleImplementation.isForIndividualMembersInputName).
      map(pv => pv.preparedValue match {
        case JsBoolean(true) => true
        case _ => false
      })
  }

  def maybeRecurrenceFor(parametersWithValues: Seq[ParameterWithValue]): Option[String] = {
    parametersWithValues.
      find(_.parameter.input.name == ScheduleImplementation.recurrenceInputName).
      flatMap(_.maybeValue.map(_.text))
  }

  def maybeFor(parametersWithValues: Seq[ParameterWithValue], event: Event, services: DefaultServices): Option[ScheduleImplementation] = {
    for {
      message <- maybeMessageTextFor(parametersWithValues)
      isForIndividualMembers <- isForIndividualMembersFor(parametersWithValues)
      recurrence <- maybeRecurrenceFor(parametersWithValues)
    } yield {
      ScheduleImplementation(message, isForIndividualMembers, recurrence, event, services)
    }
  }

}
