package controllers


import javax.inject.Inject

import models.behaviors.defaultstorageitem.DefaultStorageItemService
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Action
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.playJson._
import sangria.parser.{QueryParser, SyntaxError}
import sangria.schema.Schema
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class GraphQLController @Inject() (
                                    val messagesApi: MessagesApi,
                                    val configuration: Configuration,
                                    val dataService: DataService
                                  ) extends EllipsisController {

  def executeQuery(schema: Schema[DefaultStorageItemService, Unit], query: Document, op: Option[String], vars: JsObject) =
    Executor.execute(schema, query, operationName = op, variables = vars, userContext = dataService.defaultStorageItems)
      .map(Ok(_))
      .recover {
        case error: QueryAnalysisError ⇒ BadRequest(error.resolveError)
        case error: ErrorWithResolver ⇒ InternalServerError(error.resolveError)
      }

  private def maybeVariablesFrom(maybeString: Option[String]): JsObject = {
    maybeString.flatMap { str =>
      Json.parse(str) match {
        case obj: JsObject => Some(obj)
        case _ => None
      }
    }.getOrElse(Json.obj())
  }

  def query(
              behaviorGroupId: String,
              token: String,
              maybeQuery: Option[String],
              maybeOperationName: Option[String],
              maybeVariables: Option[String]
              ) = Action.async(parse.json) { request ⇒

    for {
      maybeTeam <- dataService.teams.findForInvocationToken(token)
      maybeBehaviorGroup <- dataService.behaviorGroups.find(behaviorGroupId)
      maybeBehaviorGroupVersion <- maybeBehaviorGroup.map { group =>
        dataService.behaviorGroups.maybeCurrentVersionFor(group)
      }.getOrElse(Future.successful(None))
      maybeSchema <- maybeBehaviorGroupVersion.map { groupVersion =>
        dataService.behaviorGroupVersions.schemaFor(groupVersion).map(Some(_))
      }.getOrElse(Future.successful(None))
      result <- (for {
        schema <- maybeSchema
        query <- maybeQuery
      } yield {
        QueryParser.parse(query) match {
          case Success(queryAst) => {
            executeQuery(schema, queryAst, maybeOperationName, maybeVariablesFrom(maybeVariables))
          }

          case Failure(error) => {
            Future.successful(BadRequest(Json.obj("error" → error.getMessage)))
          }
        }
      }).getOrElse(Future.successful(NotFound("")))
    } yield result
  }

}
