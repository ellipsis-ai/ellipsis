package services

import javax.inject.Inject

import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import play.api.libs.json.{JsObject, JsValue, Json}
import sangria.ast.Document
import sangria.execution.Executor
import sangria.marshalling.playJson._
import sangria.parser.QueryParser
import sangria.schema.{DefaultAstSchemaBuilder, Schema}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class GraphQLServiceImpl @Inject() (
                                    dataService: DataService
                                  ) extends GraphQLService {

  private def schemaStringFor(groupVersion: BehaviorGroupVersion): Future[String] = {
    for {
      configs <- dataService.dataTypeConfigs.allFor(groupVersion).map(_.sortBy(_.id))
      typesStr <- Future.sequence(configs.map(_.graphQL(dataService))).map(_.mkString("\n\n"))
    } yield {
      val queryFieldsStr = configs.map(_.graphQLQueryFieldsString).mkString("")
      val mutationFieldsStr = configs.map(_.graphQLMutationFieldsString).mkString("")
      s"""schema {
         |  query: Query
         |  mutation: Mutation
         |}
         |
         |type Query {
         |$queryFieldsStr
         |}
         |
         |type Mutation {
         |$mutationFieldsStr
         |}
         |
         |$typesStr
         |
         |
       """.stripMargin
    }
  }

  class MySchemaBuilder extends DefaultAstSchemaBuilder[DefaultStorageItemService]

  def schemaFor(groupVersion: BehaviorGroupVersion): Future[Schema[DefaultStorageItemService, Any]] = {
    schemaStringFor(groupVersion).map { str =>
      QueryParser.parse(str) match {
        case Success(res) => Schema.buildFromAst(res, new MySchemaBuilder)
        case Failure(err) => throw new RuntimeException(err.getMessage)
      }
    }
  }

  def executeQuery(
                    schema: Schema[DefaultStorageItemService, Any],
                    query: Document,
                    op: Option[String],
                    vars: JsObject
                  ): Future[JsValue] = {
    Executor.execute(schema, query, operationName = op, variables = vars, userContext = dataService.defaultStorageItems)
  }

  private def maybeVariablesFrom(maybeString: Option[String]): JsObject = {
    maybeString.flatMap { str =>
      Json.parse(str) match {
        case obj: JsObject => Some(obj)
        case _ => None
      }
    }.getOrElse(Json.obj())
  }

  def runQuery(
               behaviorGroup: BehaviorGroup,
               query: String,
               maybeOperationName: Option[String],
               maybeVariables: Option[String]
              ): Future[Option[JsValue]] = {

    for {
      maybeBehaviorGroupVersion <- dataService.behaviorGroups.maybeCurrentVersionFor(behaviorGroup)
      maybeSchema <- maybeBehaviorGroupVersion.map { groupVersion =>
        schemaFor(groupVersion).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeResult <- {
        maybeSchema.map { schema =>
          QueryParser.parse(query) match {
            case Success(queryAst) => {
              executeQuery(schema, queryAst, maybeOperationName, maybeVariablesFrom(maybeVariables)).map(Some(_))
            }
            case Failure(error) => throw error
          }
        }.getOrElse(Future.successful(None))
      }
    } yield maybeResult
  }

}
