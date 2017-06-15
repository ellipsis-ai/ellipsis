package services

import javax.inject.Inject

import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.defaultstorageitem.DefaultStorageItemService
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
}
