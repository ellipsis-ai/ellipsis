package models.behaviors.conversations

import akka.actor.ActorSystem
import play.api.Configuration
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, CacheService, DataService, SlackEventService}

case class ConversationServices(
                                 dataService: DataService,
                                 lambdaService: AWSLambdaService,
                                 slackEventService: SlackEventService,
                                 cacheService: CacheService,
                                 configuration: Configuration,
                                 ws: WSClient,
                                 actorSystem: ActorSystem
                              )
