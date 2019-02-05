package controllers.api.context

import akka.actor.ActorSystem
import controllers.api.APIResponder
import controllers.api.exceptions.APIMethodContextBuilderException
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

object ApiMethodContextBuilder {

  def createFor(
                 token: String,
                 services: DefaultServices,
                 responder: APIResponder
               )(implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[ApiMethodContext] = {
    // TODO: This method uses subtle implicit logic to figure out which kind of context we're in
    // It should instead use the responseContext parameter sent by the client
    SlackApiMethodContext.maybeCreateFor(token, services, responder).flatMap { maybeSlackMethodContext: Option[ApiMethodContext] =>
      maybeSlackMethodContext.map(Future.successful).getOrElse {
        MSTeamsApiMethodContext.maybeCreateFor(token, services, responder).flatMap { maybeSlackMethodContext: Option[ApiMethodContext] =>
          maybeSlackMethodContext.map(Future.successful).getOrElse {
            NoMediumApiMethodContext.maybeCreateFor(token, services, responder).map { maybeNoMediumMethodContext =>
              maybeNoMediumMethodContext.getOrElse {
                throw new APIMethodContextBuilderException
              }
            }
          }
        }
      }
    }
  }

}
