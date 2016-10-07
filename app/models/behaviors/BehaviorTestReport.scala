package models.behaviors

import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.{MessageContext, MessageEvent}
import play.api.libs.json._
import services.DataService

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable.ArrayBuffer

case class TestMessageContext(fullMessageText: String, includesBotMention: Boolean) extends MessageContext {

  val messageBuffer: ArrayBuffer[String] = new ArrayBuffer()
  override def relevantMessageText: String = fullMessageText
  val userIdForContext = "test"
  val name = "test"
  val teamId = "test"
  val isResponseExpected = true

  def maybeOngoingConversation(dataService: DataService): Future[Option[Conversation]] = Future.successful(None)

  def sendMessage(text: String, forcePrivate: Boolean = false, maybeShouldUnfurl: Option[Boolean] = None)(implicit ec: ExecutionContext): Unit = {
    messageBuffer += text
  }

}

case class TestEvent(context: TestMessageContext) extends MessageEvent

case class BehaviorTestReportOutput(
                                     message: String,
                                     activatedTrigger: Option[String],
                                     paramValues: Map[String, Option[String]],
                                     responseMessage: Option[String]
                                     )

case class BehaviorTestReport  (
                               event: TestEvent,
                               behaviorVersion: BehaviorVersion,
                               maybeBehaviorResponse: Option[BehaviorResponse]
                               ) {

  val maybeActivatedTrigger = maybeBehaviorResponse.map(_.activatedTrigger)

  def messages: Array[String] = event.context.messageBuffer.toArray

  def paramValues: Map[String, Option[String]] = maybeBehaviorResponse.map { behaviorResponse =>
    behaviorResponse.parametersWithValues.map { p =>
      (p.parameter.name, p.maybeValue.map(value => Some(value.text)).getOrElse(None))
    }.toMap
  }.getOrElse(Map())

  implicit val outputWrites = Json.writes[BehaviorTestReportOutput]

  def json: JsValue = {
    val data = BehaviorTestReportOutput(
      event.context.fullMessageText,
      maybeActivatedTrigger.map(trigger => Some(trigger.pattern)).getOrElse(None),
      paramValues,
      messages.headOption
    )
    Json.toJson(data)
  }
}
