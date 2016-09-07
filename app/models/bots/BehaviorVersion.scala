package models.bots

import java.nio.ByteBuffer
import java.nio.charset.Charset

import com.github.tototoshi.slick.PostgresJodaSupport._
import json.BehaviorVersionData
import models.accounts.user.{User, UserQueries}
import models.bots.config.{AWSConfig, AWSConfigQueries, RequiredOAuth2ApiConfigQueries}
import models.bots.events.MessageEvent
import models.bots.triggers.MessageTriggerQueries
import models.IDs
import models.environmentvariable.EnvironmentVariable
import models.team.Team
import org.commonmark.node.{AbstractVisitor, Image}
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import play.api.Configuration
import services.AWSLambdaConstants._
import services.{AWSLambdaLogResult, AWSLambdaService, DataService}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CommonmarkVisitor extends AbstractVisitor {
  override def visit(image: Image) {
    image.unlink()
  }
}

case class BehaviorVersion(
                     id: String,
                     behavior: Behavior,
                     maybeDescription: Option[String],
                     maybeShortName: Option[String],
                     maybeFunctionBody: Option[String],
                     maybeResponseTemplate: Option[String],
                     maybeAuthor: Option[User],
                     createdAt: DateTime
                     ) {

  val team: Team = behavior.team

  private def environmentVariablesUsedInConfig(dataService: DataService): DBIO[Seq[String]] = {
    AWSConfigQueries.maybeFor(this, dataService).map { maybeAwsConfig =>
      maybeAwsConfig.map { awsConfig =>
        awsConfig.environmentVariableNames
      }.getOrElse(Seq())
    }
  }

  def knownEnvironmentVariablesUsed(dataService: DataService): DBIO[Seq[String]] = {
    environmentVariablesUsedInConfig(dataService).map { inConfig =>
      inConfig ++ BehaviorVersionQueries.environmentVariablesUsedInCode(functionBody)
    }
  }

  def missingEnvironmentVariablesIn(environmentVariables: Seq[EnvironmentVariable], dataService: DataService): DBIO[Seq[String]] = {
    knownEnvironmentVariablesUsed(dataService).map{ used =>
      used diff environmentVariables.filter(_.value.trim.nonEmpty).map(_.name)
    }
  }

  // TODO: make this real
  def isInDevelopmentMode: Boolean = true

  def isSkill: Boolean = {
    maybeFunctionBody.map { body =>
      Option(body).filter(_.trim.nonEmpty).isDefined
    }.getOrElse(false)
  }

  def editLinkFor(configuration: Configuration): Option[String] = {
    configuration.getString("application.apiBaseUrl").map { baseUrl =>
      val path = controllers.routes.BehaviorEditorController.edit(behavior.id)
      s"$baseUrl$path"
    }
  }

  def description: String = maybeDescription.getOrElse("")

  def functionBody: String = maybeFunctionBody.getOrElse("")

  def awsParamsFor(maybeAWSConfig: Option[AWSConfig]): Array[String] = {
    maybeAWSConfig.map(_ => Array("AWS")).getOrElse(Array())
  }

  def functionWithParams(params: Array[String]): String = {
    val definitionUserParamsString = if (params.isEmpty) {
      ""
    } else {
      s"""\n${params.map(ea => ea ++ ",").mkString("\n")}\n"""
    }
    val possibleEndOfParamsNewline = if (params.isEmpty) { "" } else { "\n" }
    s"""function($definitionUserParamsString$CONTEXT_PARAM$possibleEndOfParamsNewline) {
      |  $functionBody
      |}""".stripMargin
  }

  def maybeFunction(dataService: DataService): DBIO[Option[String]] = {
    maybeFunctionBody.map { functionBody =>
      (for {
        params <- BehaviorParameterQueries.allFor(this)
        maybeAWSConfig <- AWSConfigQueries.maybeFor(this, dataService)
        requiredOAuth2ApiConfigs <- RequiredOAuth2ApiConfigQueries.allFor(this)
      } yield {
        functionWithParams(params.map(_.name).toArray)
      }).map(Some(_))
    }.getOrElse(DBIO.successful(None))
  }

  def functionName: String = id

  def resultFor(parametersWithValues: Seq[ParameterWithValue], event: MessageEvent, service: AWSLambdaService, dataService: DataService): Future[BehaviorResult] = {
    for {
      envVars <- dataService.environmentVariables.allFor(team)
      result <- service.invoke(this, parametersWithValues, envVars, event)
    } yield result
  }

  def unlearn(lambdaService: AWSLambdaService): DBIO[Unit] = {
    lambdaService.deleteFunction(id)
    BehaviorVersionQueries.delete(this).map(_ => Unit)
  }

  def redeploy(lambdaService: AWSLambdaService, dataService: DataService): DBIO[Unit] = {
    for {
      params <- BehaviorParameterQueries.allFor(this)
      maybeAWSConfig <- AWSConfigQueries.maybeFor(this, dataService)
      requiredOAuth2ApiConfigs <- RequiredOAuth2ApiConfigQueries.allFor(this)
      _ <- DBIO.from(
        lambdaService.deployFunctionFor(
          this,
          functionBody,
          params.map(_.name).toArray,
          maybeAWSConfig,
          requiredOAuth2ApiConfigs
        )
      )
    } yield Unit
  }

  def isCurrentVersion: Boolean = behavior.maybeCurrentVersionId.contains(id)

  private def isUnhandledError(json: JsValue): Boolean = {
    (json \ "errorMessage").toOption.flatMap { m =>
      "Process exited before completing request".r.findFirstIn(m.toString)
    }.isDefined
  }

  private def isSyntaxError(json: JsValue): Boolean = {
    (json \ "errorType").toOption.flatMap { m =>
      "SyntaxError".r.findFirstIn(m.toString)
    }.isDefined
  }

  def resultFor(payload: ByteBuffer, logResult: AWSLambdaLogResult, parametersWithValues: Seq[ParameterWithValue]): BehaviorResult = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val json = Json.parse(jsonString)
    val logResultOption = Some(logResult)
    (json \ "result").toOption.map { successResult =>
      SuccessResult(successResult, parametersWithValues, maybeResponseTemplate, logResultOption)
    }.getOrElse {
      if ((json \ NO_RESPONSE_KEY).toOption.exists(_.as[Boolean])) {
        NoResponseResult(logResultOption)
      } else {
        if (isUnhandledError(json)) {
          UnhandledErrorResult(logResultOption)
        } else if (json.toString == "null") {
          new NoCallbackTriggeredResult()
        } else if (isSyntaxError(json)) {
          SyntaxErrorResult(json, logResultOption)
        } else {
          HandledErrorResult(json, logResultOption)
        }
      }
    }
  }

  def save: DBIO[BehaviorVersion] = BehaviorVersionQueries.save(this)

  def toRaw: RawBehaviorVersion = {
    RawBehaviorVersion(id, behavior.id, maybeDescription, maybeShortName, maybeFunctionBody, maybeResponseTemplate, maybeAuthor.map(_.id), createdAt)
  }

}

case class RawBehaviorVersion(
                        id: String,
                        behaviorId: String,
                        maybeDescription: Option[String],
                        maybeShortName: Option[String],
                        maybeFunctionBody: Option[String],
                        maybeResponseTemplate: Option[String],
                        maybeAuthorId: Option[String],
                        createdAt: DateTime
                        )

class BehaviorVersionsTable(tag: Tag) extends Table[RawBehaviorVersion](tag, "behavior_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorId = column[String]("behavior_id")
  def maybeDescription = column[Option[String]]("description")
  def maybeShortName = column[Option[String]]("short_name")
  def maybeFunctionBody = column[Option[String]]("code")
  def maybeResponseTemplate = column[Option[String]]("response_template")
  def maybeAuthorId = column[Option[String]]("author_id")
  def createdAt = column[DateTime]("created_at")

  def * =
    (id, behaviorId, maybeDescription, maybeShortName, maybeFunctionBody, maybeResponseTemplate, maybeAuthorId, createdAt) <>
      ((RawBehaviorVersion.apply _).tupled, RawBehaviorVersion.unapply _)
}

object BehaviorVersionQueries {

  def all = TableQuery[BehaviorVersionsTable]
  def allWithUser = all.joinLeft(UserQueries.all).on(_.maybeAuthorId === _.id)
  def allWithBehavior = allWithUser.join(BehaviorQueries.allWithTeam).on(_._1.behaviorId === _._1.id)

  type TupleType = ((RawBehaviorVersion, Option[User]), (RawBehavior, Team))

  def tuple2BehaviorVersion(tuple: TupleType): BehaviorVersion = {
    val raw = tuple._1._1
    BehaviorVersion(
      raw.id,
      BehaviorQueries.tuple2Behavior(tuple._2),
      raw.maybeDescription,
      raw.maybeShortName,
      raw.maybeFunctionBody,
      raw.maybeResponseTemplate,
      tuple._1._2,
      raw.createdAt
    )
  }

  def allOfThem: DBIO[Seq[BehaviorVersion]] = {
    allWithBehavior.result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
  }

  def uncompiledCurrentWithFunctionQuery() = {
    allWithBehavior.
      filter { case((version, _), (behavior, team)) => behavior.maybeCurrentVersionId === version.id}.
      filter { case((version, _), _) => version.maybeFunctionBody.map(_.trim.length > 0).getOrElse(false) }.
      map { case((version, _), _) => version.id }
  }
  val currentWithFunctionQuery = Compiled(uncompiledCurrentWithFunctionQuery)

  def currentIdsWithFunction: DBIO[Seq[String]] = {
    uncompiledCurrentWithFunctionQuery.result
  }

  def uncompiledAllForQuery(behaviorId: Rep[String]) = {
    allWithBehavior.
      filter { case((version, _), _) => version.behaviorId === behaviorId }.
      sortBy { case((version, _), _) => version.createdAt.desc }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(behavior: Behavior): DBIO[Seq[BehaviorVersion]] = {
    allForQuery(behavior.id).result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithBehavior.filter { case((version, _), _) => version.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def findWithoutAccessCheck(id: String): DBIO[Option[BehaviorVersion]] = {
    findQuery(id).result.map(_.headOption.map(tuple2BehaviorVersion))
  }

  def createFor(behavior: Behavior, maybeUser: Option[User]): DBIO[BehaviorVersion] = {
    val raw = RawBehaviorVersion(IDs.next, behavior.id, None, None, None, None, maybeUser.map(_.id), DateTime.now)

    (all += raw).map { _ =>
      BehaviorVersion(raw.id, behavior, raw.maybeDescription, raw.maybeShortName, raw.maybeFunctionBody, raw.maybeResponseTemplate, maybeUser, raw.createdAt)
    }
  }

  def createFor(
                 behavior: Behavior,
                 maybeUser: Option[User],
                 lambdaService: AWSLambdaService,
                 data: BehaviorVersionData,
                 dataService: DataService): DBIO[BehaviorVersion] = {
    (for {
      behaviorVersion <- createFor(behavior, maybeUser)
      _ <-
        for {
          updated <- behaviorVersion.copy(
            maybeFunctionBody = Some(data.functionBody),
            maybeResponseTemplate = Some(data.responseTemplate)
          ).save
          maybeAWSConfig <- data.awsConfig.map { c =>
            AWSConfigQueries.createFor(updated, c.accessKeyName, c.secretKeyName, c.regionName, dataService).map(Some(_))
          }.getOrElse(DBIO.successful(None))
          requiredOAuth2ApiConfigs <- DBIO.sequence(data.config.requiredOAuth2ApiConfigs.getOrElse(Seq()).map { requiredData =>
            RequiredOAuth2ApiConfigQueries.maybeCreateFor(requiredData, updated)
          }).map(_.flatten)
          _ <- DBIO.from(lambdaService.deployFunctionFor(
            updated,
            data.functionBody,
            BehaviorVersionQueries.withoutBuiltin(data.params.map(_.name).toArray),
            maybeAWSConfig,
            requiredOAuth2ApiConfigs
          ))
          _ <- BehaviorParameterQueries.ensureFor(updated, data.params.map(ea => (ea.name, Some(ea.question))))
          _ <- DBIO.sequence(
            data.triggers.
              filterNot(_.text.trim.isEmpty)
              map { trigger =>
              MessageTriggerQueries.createFor(updated, trigger.text, trigger.requiresMention, trigger.isRegex, trigger.caseSensitive)
            }
          )
        } yield Unit
    } yield behaviorVersion) transactionally
  }

  def uncompiledFindQueryFor(id: Rep[String]) = all.filter(_.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def save(behaviorVersion: BehaviorVersion): DBIO[BehaviorVersion] = {
    val raw = behaviorVersion.toRaw
    val query = findQueryFor(raw.id)
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse(all += raw)
    }.map(_ => behaviorVersion)
  }

  def delete(behaviorVersion: BehaviorVersion): DBIO[BehaviorVersion] = {
    all.filter(_.id === behaviorVersion.id).delete.map(_ => behaviorVersion)
  }

  private def paramsIn(code: String): Array[String] = {
    """.*function\s*\(([^\)]*)\)""".r.findFirstMatchIn(code).flatMap { firstMatch =>
      firstMatch.subgroups.headOption.map { paramString =>
        paramString.split("""\s*,\s*""").filter(_.nonEmpty)
      }
    }.getOrElse(Array())
  }

  import services.AWSLambdaConstants._

  def withoutBuiltin(params: Array[String]) = params.filterNot(ea => ea == ON_SUCCESS_PARAM || ea == ON_ERROR_PARAM || ea == CONTEXT_PARAM)

  val functionBodyRegex = """(?s)^\s*function\s*\([^\)]*\)\s*\{(.*)\}$""".r

  def environmentVariablesUsedInCode(functionBody: String): Seq[String] = {
    // regex quite incomplete, but we're just trying to provide some guidance
    """(?s)ellipsis\.env\.([$A-Za-z_][0-9A-Za-z_$]*)""".r.findAllMatchIn(functionBody).flatMap { m =>
      m.subgroups.headOption
    }.toSeq
  }

}
