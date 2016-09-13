package models.bots.events

import javax.inject._

import models.bots.{BehaviorResponse, BehaviorResult, NoResponseResult, SimpleTextResult}
import models.bots.builtins.BuiltinBehavior
import models.bots.conversations.Conversation
import play.api.i18n.MessagesApi
import services.{AWSLambdaService, DataService}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EventHandler @Inject() (
                               lambdaService: AWSLambdaService,
                               dataService: DataService,
                               messages: MessagesApi
                               ) {

  def startInvokeConversationFor(event: MessageEvent): DBIO[BehaviorResult] = {
    val context = event.context
    for {
      maybeTeam <- DBIO.from(dataService.teams.find(context.teamId))
      maybeResponse <- BehaviorResponse.chooseFor(event, maybeTeam, None)
      result <- maybeResponse.map { response =>
        response.result(lambdaService, dataService)
      }.getOrElse {
        val result = if (context.isResponseExpected) {
          SimpleTextResult(context.iDontKnowHowToRespondMessageFor(lambdaService))
        } else {
          NoResponseResult(None)
        }
        DBIO.successful(result)
      }
    } yield result
  }

  def handleInConversation(conversation: Conversation, event: MessageEvent): DBIO[BehaviorResult] = {
    conversation.resultFor(event, lambdaService, dataService)
  }

  def handle(event: Event): Future[BehaviorResult] = {
    event match {
      case messageEvent: MessageEvent => {
        val action = for {
          maybeConversation <- event.context.maybeOngoingConversation
          result <- maybeConversation.map { conversation =>
            handleInConversation(conversation, messageEvent)
          }.getOrElse {
            BuiltinBehavior.maybeFrom(messageEvent.context, lambdaService, dataService).map { builtin =>
              DBIO.from(builtin.result)
            }.getOrElse {
              startInvokeConversationFor(messageEvent)
            }
          }
        } yield result
        dataService.run(action)
      }
      case _ => Future.successful(NoResponseResult(None))
    }

  }
}
