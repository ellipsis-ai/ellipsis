package models.behaviors

import akka.actor.ActorSystem
import models.behaviors.behavior.Behavior
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.behaviors.triggers.messagetrigger.MessageTrigger
import models.team.Team
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService}
import slick.dbio.DBIO

import scala.concurrent.Future

trait BehaviorResponseService {

  def parametersWithValuesFor(
                               event: Event,
                               behaviorVersion: BehaviorVersion,
                               paramValues: Map[String, String],
                               maybeConversation: Option[Conversation],
                               dataService: DataService,
                               cache: CacheApi,
                               configuration: Configuration,
                               actorSystem: ActorSystem
                             ): Future[Seq[ParameterWithValue]]

  def buildForAction(
                      event: Event,
                      behaviorVersion: BehaviorVersion,
                      paramValues: Map[String, String],
                      maybeActivatedTrigger: Option[MessageTrigger],
                      maybeConversation: Option[Conversation],
                      lambdaService: AWSLambdaService,
                      dataService: DataService,
                      cache: CacheApi,
                      ws: WSClient,
                      configuration: Configuration,
                      actorSystem: ActorSystem
                    ): DBIO[BehaviorResponse]

  def buildFor(
                event: Event,
                behaviorVersion: BehaviorVersion,
                paramValues: Map[String, String],
                maybeActivatedTrigger: Option[MessageTrigger],
                maybeConversation: Option[Conversation],
                lambdaService: AWSLambdaService,
                dataService: DataService,
                cache: CacheApi,
                ws: WSClient,
                configuration: Configuration,
                actorSystem: ActorSystem
              ): Future[BehaviorResponse]

  def allFor(
              event: Event,
              maybeTeam: Option[Team],
              maybeLimitToBehavior: Option[Behavior],
              lambdaService: AWSLambdaService,
              dataService: DataService,
              cache: CacheApi,
              ws: WSClient,
              configuration: Configuration,
              actorSystem: ActorSystem
            ): Future[Seq[BehaviorResponse]] = {
    event.allBehaviorResponsesFor(maybeTeam, maybeLimitToBehavior, lambdaService, dataService, cache, ws, configuration, actorSystem)
  }

}
