package services

import javax.inject.Inject
import json.BehaviorGroupData
import models.accounts.user.User
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorparameter.YesNoType
import models.behaviors.datatypeconfig.BehaviorVersionForDataTypeSchema
import models.behaviors.defaultstorageitem.DefaultStorageItemService
import play.api.Logger
import play.api.libs.json._
import sangria.ast
import sangria.ast.Document
import sangria.execution.{Executor, UserFacingError}
import sangria.marshalling.playJson._
import sangria.parser.{QueryParser, SyntaxError}
import sangria.schema.{Action, Context, DefaultAstSchemaBuilder, Schema}
import services.caching.{CacheService, DefaultStorageSchemaCacheKey}

import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class ItemNotFoundError(id: String) extends Exception with UserFacingError {
  override def getMessage(): String = s"Item with ID `$id` not found"
}

class GraphQLServiceImpl @Inject() (
                                    dataService: DataService,
                                    cacheService: CacheService,
                                    implicit val ec: ExecutionContext
                                  ) extends GraphQLService {

  private def buildSchemaStringFor(versions: Seq[BehaviorVersionForDataTypeSchema]): Future[String] = {
    val queryFieldsStr = versions.map(_.queryFieldsString).mkString("")
    val mutationFieldsStr = versions.map(_.mutationFieldsString).mkString("")
    Future.sequence(versions.map(_.graphQL(dataService))).map(_.mkString("\n\n")).map { typesStr =>
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

  private def schemaStringFor(groupVersionId: String): Future[String] = {
    for {
      configs <- dataService.dataTypeConfigs.allUsingDefaultStorageFor(groupVersionId).map(_.sortBy(_.id))
      behaviorVersions <- Future.sequence(configs.map { ea =>
        dataService.behaviorVersions.findWithoutAccessCheck(ea.behaviorVersion.id)
      }).map(_.flatten)
      schemaString <- buildSchemaStringFor(behaviorVersions)
    } yield schemaString
  }

  private def previewSchemaStringFor(data: BehaviorGroupData): Future[String] = {
    buildSchemaStringFor(data.dataTypeBehaviorVersions)
  }

  class MySchemaBuilder(groupVersion: BehaviorGroupVersion, user: User) extends DefaultAstSchemaBuilder[DefaultStorageItemService] {

    val group = groupVersion.group

    val listFieldRegex = """(\S+)List""".r
    val createFieldRegex = """create(\S+)""".r
    val updateFieldRegex = """update(\S+)""".r
    val deleteFieldRegex = """delete(\S+)""".r
    val deleteWhereFieldRegex = """deleteWhere(\S+)""".r

    private def toJson(v: Any): JsValue = {
      v match {
        case opt: Option[Any] => opt.map(toJson).getOrElse(JsNull)
        case s: String => Json.toJson(s)
        case n: Double => JsNumber(BigDecimal(n))
        case b: Boolean => JsBoolean(b)
        case arr: Array[Any] => JsArray(arr.map(toJson))
        case m: Map[_, _] => JsObject(m.map {
          case(key, value) => {
            try {
              (key.asInstanceOf[String], toJson(value))
            } catch {
              case e: ClassCastException => {
                Logger.warn(s"GraphQL toJson called with a Map with non-string keys. Forcing keys to strings:\n$m", e)
                (key.toString, toJson(value))
              }
            }
          }
        })
        case _ => JsNull
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
          ctx.ctx.filter(typeName.capitalize, valueFor(ctx, definition), groupVersion).map { items =>
            fromJson(JsArray(items.map(_.data)))
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
        case createFieldRegex(typeName) => ctx.ctx.createItem(typeName, user, valueFor(ctx, definition), groupVersion).map(_.data)
        case updateFieldRegex(typeName) => ctx.ctx.updateItem(typeName, user, valueFor(ctx, definition), groupVersion).map(_.data)
        case deleteWhereFieldRegex(typeName) => {
          ctx.ctx.deleteFilteredItemsFor(typeName, valueFor(ctx, definition), groupVersion).map { items =>
            items.map(_.data)
          }
        }
        case deleteFieldRegex(_) => {
          val idToDelete: String = ctx.arg(definition.arguments.head.name)
          ctx.ctx.deleteItem(idToDelete, groupVersion).map { maybeItem =>
            maybeItem.map(_.data).getOrElse {
              throw ItemNotFoundError(idToDelete)
            }
          }
        }
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
              case JsNull => null
              case _ => {
                val maybeValue = (ctx.value.asInstanceOf[JsObject] \ (definition.name)).asOpt[JsValue].map(fromJson)
                if (definition.fieldType.namedType.name == "Boolean") {
                  parseBoolean(maybeValue)
                } else {
                  maybeValue
                }
              }
            }
          }
        }
      }
    }

    private def parseBoolean(maybeValue: Option[Any]) = {
      maybeValue.map {
        case b: Boolean => b
        case s: String => YesNoType.maybeValidValueFor(s).getOrElse(null)
        case d: Double =>
          if (d == 1) {
            true
          } else if (d == 0) {
            false
          } else {
            null
          }
        case _ => null
      }.orNull
    }

    def fromJson(v: JsValue) = v match {
      case JsArray(l) ⇒ l
      case JsString(s) ⇒ s
      case JsNumber(n) ⇒ {
        n.doubleValue()
      }
      case JsBoolean(b) => b
      case JsNull => null
      case other ⇒ other
    }
  }

  def buildSchemaFor(key: DefaultStorageSchemaCacheKey): Future[Schema[DefaultStorageItemService, Any]] = {
    for {
      schema <- schemaStringFor(key.groupVersion.id).map { str =>
        QueryParser.parse(str) match {
          case Success(res) => Schema.buildFromAst(res, new MySchemaBuilder(key.groupVersion, key.user))
          case Failure(err) => throw err
        }
      }
    } yield schema
  }

  def schemaFor(groupVersion: BehaviorGroupVersion, user: User): Future[Schema[DefaultStorageItemService, Any]] = {
    val key = DefaultStorageSchemaCacheKey(groupVersion, user)
    cacheService.getDefaultStorageSchema(key, buildSchemaFor)
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
      case e: sangria.execution.UserFacingError => errorResultFor(e.getMessage)
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
      "errors" -> Seq(Map(
        "message" -> message
      ))
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
        (for {
          schema <- schemaFor(groupVersion, user)
          result <- QueryParser.parse(query) match {
            case Success(queryAst) => {
              executeQuery(schema, queryAst, maybeOperationName, variablesFrom(maybeVariables))
            }
            case Failure(error) => errorResultFutureFor(error.getMessage)
          }
        } yield result).recover {
          case err: SyntaxError => errorResultFor(err.getMessage())
          case t: Throwable => throw t
        }
      }.getOrElse {
        errorResultFutureFor("Couldn't find a saved version for this skill")
      }
    }
  }

}
