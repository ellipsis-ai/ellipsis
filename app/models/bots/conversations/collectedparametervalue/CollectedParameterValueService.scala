package models.bots.conversations.collectedparametervalue

import models.bots.conversations.conversation.Conversation

import scala.concurrent.Future

trait CollectedParameterValueService {

  def allFor(conversation: Conversation): Future[Seq[CollectedParameterValue]]

  def save(value: CollectedParameterValue): Future[CollectedParameterValue]

}
