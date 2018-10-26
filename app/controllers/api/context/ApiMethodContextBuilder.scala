package controllers.api.context

import akka.actor.ActorSystem
import controllers.api.exceptions.APIMethodContextBuilderException
import services.DefaultServices

import scala.concurrent.{ExecutionContext, Future}

object ApiMethodContextBuilder {

  def createFor(
                 token: String,
                 services: DefaultServices
               )(implicit ec: ExecutionContext, actorSystem: ActorSystem): Future[ApiMethodContext] = {
    SlackApiMethodContext.maybeCreateFor(token, services).flatMap { maybeSlackMethodContext: Option[ApiMethodContext] =>
      maybeSlackMethodContext.map(Future.successful).getOrElse {
        NoMediumApiMethodContext.maybeCreateFor(token, services).map { maybeNoMediumMethodContext =>
          maybeNoMediumMethodContext.getOrElse {
            throw new APIMethodContextBuilderException
          }
        }
      }
    }
  }

}
