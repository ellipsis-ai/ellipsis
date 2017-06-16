package services

import javax.inject.Inject

import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import play.api.libs.json._
import sangria.ast
import sangria.ast.Document
import sangria.execution.Executor
import sangria.marshalling.playJson._
import sangria.parser.QueryParser
import sangria.schema.{Action, Context, DefaultAstSchemaBuilder, Schema}

import scala.collection.immutable.ListMap
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
      val queryFieldsStr = configs.map(_.queryFieldsString).mkString("")
      val mutationFieldsStr = configs.map(_.mutationFieldsString).mkString("")
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

  class MySchemaBuilder(groupVersion: BehaviorGroupVersion) extends DefaultAstSchemaBuilder[DefaultStorageItemService] {

    val group = groupVersion.group

    val listFieldRegex = """(\S+)List""".r
    val createFieldRegex = """create(\S+)""".r
    val deleteFieldRegex = """delete(\S+)""".r


    private def resolveQueryField(
                                   ctx: Context[DefaultStorageItemService, _],
                                   typeDefinition: ast.TypeDefinition,
                                   definition: ast.FieldDefinition
                                 ): Context[DefaultStorageItemService, _] => Action[DefaultStorageItemService, _] = {
      definition.name match {
        case listFieldRegex(typeName) => ctx => ctx.ctx.filter(typeName, ctx.arg(definition.arguments.head.name), group)
      }
    }

    private def resolveMutationField(
                                   ctx: Context[DefaultStorageItemService, _],
                                   typeDefinition: ast.TypeDefinition,
                                   definition: ast.FieldDefinition
                                 ): Action[DefaultStorageItemService, _] = {
      definition.name match {
        case createFieldRegex(typeName) => {
          val data: JsValue = Json.toJson(ctx.arg(definition.arguments.head.name).asInstanceOf[ListMap[String, Option[String]]].toArray.toMap)
          ctx.ctx.createItem(typeName, data, group).map { newItem =>
            newItem.data
          }
        }
        case deleteFieldRegex(_) => ctx.ctx.deleteItem(ctx.arg(definition.arguments.head.name), group)
      }
    }

    override def resolveField(
                               typeDefinition: ast.TypeDefinition,
                               definition: ast.FieldDefinition
                             ): Context[DefaultStorageItemService, _] => Action[DefaultStorageItemService, _] = {
      ctx => {
        typeDefinition.name match {
          case "Query" => resolveQueryField(ctx, typeDefinition, definition)
          case "Mutation" => resolveMutationField(ctx, typeDefinition, definition)
          case _ => {
            val jsVal = (ctx.value.asInstanceOf[JsObject] \ (definition.name)).get
            fromJson(jsVal)
          }
        }
      }
    }

    def fromJson(v: JsValue) = v match {
      case JsArray(l) ⇒ l
      case JsString(s) ⇒ s
      case JsNumber(n) ⇒ n.intValue()
      case other ⇒ other
    }
  }

  def schemaFor(groupVersion: BehaviorGroupVersion): Future[Schema[DefaultStorageItemService, Any]] = {
    schemaStringFor(groupVersion).map { str =>
      QueryParser.parse(str) match {
        case Success(res) => Schema.buildFromAst(res, new MySchemaBuilder(groupVersion))
        case Failure(err) => throw new RuntimeException(err.getMessage)
      }
    }
  }

  def executeQuery(
                    schema: Schema[DefaultStorageItemService, Any],
                    query: Document,
                    op: Option[String],
                    vars: JsValue
                  ): Future[JsValue] = {
    Executor.execute(schema, query, operationName = op, variables = vars, userContext = dataService.defaultStorageItems)
  }

  private def variablesFrom(maybeString: Option[String]): JsValue = {
    maybeString.flatMap { str =>
      Json.parse(str) match {
        case obj: JsObject => Some(obj)
        case _ => None
      }
    }.getOrElse(JsNull)
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
              executeQuery(schema, queryAst, maybeOperationName, variablesFrom(maybeVariables)).map(Some(_))
            }
            case Failure(error) => throw error
          }
        }.getOrElse(Future.successful(None))
      }
    } yield maybeResult
  }

}
