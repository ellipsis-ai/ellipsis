package models.behaviors.conversations.collectedparametervalue

import models.behaviors.conversations.conversation.Conversation

import scala.concurrent.Future

trait CollectedParameterValueService {

  def allFor(conversation: Conversation): Future[Seq[CollectedParameterValue]]

  def save(value: CollectedParameterValue): Future[CollectedParameterValue]

}
