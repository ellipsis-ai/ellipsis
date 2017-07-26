package models.behaviors.conversations

import akka.actor.ActorSystem
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaService, DataService, SlackEventService}

case class ConversationServices(
                                dataService: DataService,
                                lambdaService: AWSLambdaService,
                                slackEventService: SlackEventService,
                                cache: CacheApi,
                                configuration: Configuration,
                                ws: WSClient,
                                actorSystem: ActorSystem
                              )
