package services

import javax.inject.Inject

import json.BehaviorGroupData
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.datatypeconfig.DataTypeConfigForSchema
import models.behaviors.defaultstorageitem.{DefaultStorageItemService, GraphQLHelpers}
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

  private def schemaStringFromConfigs(configs: Seq[DataTypeConfigForSchema]): Future[String] = {
    val queryFieldsStr = configs.map(_.queryFieldsString).mkString("")
    val mutationFieldsStr = configs.map(_.mutationFieldsString).mkString("")
    Future.sequence(configs.map(_.graphQL(dataService))).map(_.mkString("\n\n")).map { typesStr =>
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

  private def schemaStringFor(groupVersion: BehaviorGroupVersion): Future[String] = {
    dataService.dataTypeConfigs.allUsingDefaultStorageFor(groupVersion).map(_.sortBy(_.id)).flatMap { configs =>
      schemaStringFromConfigs(configs)
    }
  }

  private def previewSchemaStringFor(data: BehaviorGroupData): Future[String] = {
    val configs = data.dataTypeBehaviorVersions.flatMap(_.dataTypeConfig)
    schemaStringFromConfigs(configs)
  }

  class MySchemaBuilder(groupVersion: BehaviorGroupVersion, user: User) extends DefaultAstSchemaBuilder[DefaultStorageItemService] {

    val group = groupVersion.group

    val listFieldRegex = """(\S+)List""".r
    val createFieldRegex = """create(\S+)""".r
    val deleteFieldRegex = """delete(\S+)""".r

    private def toJson(v: Any): JsValue = {
      v match {
        case opt: Option[Any] => opt.map(toJson).getOrElse(JsNull)
        case s: String => Json.toJson(s)
        case n: Double => JsNumber(BigDecimal(n))
        case arr: Array[Any] => JsArray(arr.map(toJson))
        case m: Map[String, Any] => {
          JsObject(m.map { ea => (ea._1, toJson(ea._2)) })
        }
      }
    }

    private def valueFor(
                          ctx: Context[DefaultStorageItemService, _],
                          definition: ast.FieldDefinition
                        ): JsValue = {
      Json.toJson(ctx.arg(definition.arguments.head.name).asInstanceOf[ListMap[String, Option[Any]]].toArray.toMap.map { case(k, maybeValue) =>
        (k, maybeValue.map(toJson))
      })
    }


    private def resolveQueryField(
                                   ctx: Context[DefaultStorageItemService, _],
                                   typeDefinition: ast.TypeDefinition,
                                   definition: ast.FieldDefinition
                                 ): Action[DefaultStorageItemService, _] = {
      definition.name match {
        case listFieldRegex(typeName) => {
          ctx.ctx.filter(typeName.capitalize, valueFor(ctx, definition), group).map { items =>
            fromJson(JsArray(items.map(_.dataWithId)))
          }
        }
      }
    }

    private def resolveMutationField(
                                   ctx: Context[DefaultStorageItemService, _],
                                   typeDefinition: ast.TypeDefinition,
                                   definition: ast.FieldDefinition
                                 ): Action[DefaultStorageItemService, _] = {
      definition.name match {
        case createFieldRegex(typeName) => ctx.ctx.createItem(typeName, user, valueFor(ctx, definition), group).map(_.dataWithId)
        case deleteFieldRegex(_) => ctx.ctx.deleteItem(ctx.arg(definition.arguments.head.name), group).map(_.dataWithId)
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
            ctx.value match {
              case arr: JsArray => fromJson(arr)
              case _ => {
                val jsVal = (ctx.value.asInstanceOf[JsObject] \ (definition.name)).get
                fromJson(jsVal)
              }
            }
          }
        }
      }
    }

    def fromJson(v: JsValue) = v match {
      case JsArray(l) ⇒ l
      case JsString(s) ⇒ s
      case JsNumber(n) ⇒ n.doubleValue()
      case other ⇒ other
    }
  }

  def schemaFor(groupVersion: BehaviorGroupVersion, user: User): Future[Schema[DefaultStorageItemService, Any]] = {
    schemaStringFor(groupVersion).map { str =>
      QueryParser.parse(str) match {
        case Success(res) => Schema.buildFromAst(res, new MySchemaBuilder(groupVersion, user))
        case Failure(err) => throw new RuntimeException(err.getMessage)
      }
    }
  }

  def previewSchemaFor(data: BehaviorGroupData): Future[Schema[DefaultStorageItemService, Any]] = {
    previewSchemaStringFor(data).map { str =>
      QueryParser.parse(str) match {
        case Success(res) => Schema.buildFromAst(res, new DefaultAstSchemaBuilder[DefaultStorageItemService]())
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
    Executor.execute(schema, query, operationName = op, variables = vars, userContext = dataService.defaultStorageItems).recover {
      case e: sangria.execution.ValidationError => errorResultFor(e.getMessage)
    }
  }

  private def variablesFrom(maybeString: Option[String]): JsValue = {
    maybeString.flatMap { str =>
      Json.parse(str) match {
        case obj: JsObject => Some(obj)
        case _ => None
      }
    }.getOrElse(Json.obj())
  }

  private def errorResultFor(message: String): JsValue = {
    Json.toJson(Map(
      "errors" -> Seq(message)
    ))
  }

  private def errorResultFutureFor(message: String): Future[JsValue] = {
    Future.successful(errorResultFor(message))
  }

  def runQuery(
               behaviorGroup: BehaviorGroup,
               user: User,
               query: String,
               maybeOperationName: Option[String],
               maybeVariables: Option[String]
              ): Future[JsValue] = {

    dataService.behaviorGroups.maybeCurrentVersionFor(behaviorGroup).flatMap { maybeBehaviorGroupVersion =>
      maybeBehaviorGroupVersion.map { groupVersion =>
        for {
          schema <- schemaFor(groupVersion, user)
          result <- QueryParser.parse(query) match {
            case Success(queryAst) => {
              executeQuery(schema, queryAst, maybeOperationName, variablesFrom(maybeVariables))
            }
            case Failure(error) => errorResultFutureFor(error.getMessage)
          }
        } yield result
      }.getOrElse {
        errorResultFutureFor("Couldn't find a saved version for this skill")
      }
    }
  }

}
