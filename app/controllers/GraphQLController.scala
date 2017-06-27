package controllers

import javax.inject.Inject

import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import services.{DataService, GraphQLService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GraphQLController @Inject() (
                                    val messagesApi: MessagesApi,
                                    val configuration: Configuration,
                                    val dataService: DataService,
                                    val graphQL: GraphQLService
                                  ) extends EllipsisController {

  def query(
              token: String,
              query: String,
              maybeOperationName: Option[String],
              maybeVariables: Option[String]
              ) = Action.async { request =>

    for {
      maybeBehaviorGroup <- dataService.behaviorGroups.findForInvocationToken(token)
      maybeResult <- maybeBehaviorGroup.map { group =>
        graphQL.runQuery(group, query, maybeOperationName, maybeVariables)
      }.getOrElse(Future.successful(None))
    } yield {
      maybeResult.map { result =>
        Ok(result.toString)
      }.getOrElse(NotFound(""))
    }
  }

}
