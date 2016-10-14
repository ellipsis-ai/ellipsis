package models.behaviors.behaviorversion

import java.nio.ByteBuffer
import java.nio.charset.Charset
import javax.inject.Inject

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.google.inject.Provider
import json.BehaviorVersionData
import models.IDs
import models.accounts.user.User
import models.behaviors.{BotResult, HandledErrorResult, NoCallbackTriggeredResult, NoResponseResult, ParameterWithValue, SuccessResult, SyntaxErrorResult, UnhandledErrorResult}
import models.behaviors.behavior.Behavior
import models.behaviors.events.MessageEvent
import models.environmentvariable.EnvironmentVariable
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import services.{AWSLambdaLogResult, AWSLambdaService, DataService}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawBehaviorVersion(
                               id: String,
                               behaviorId: String,
                               maybeDescription: Option[String],
                               maybeShortName: Option[String],
                               maybeFunctionBody: Option[String],
                               maybeResponseTemplate: Option[String],
                               forcePrivateResponse: Boolean,
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
  def forcePrivateResponse = column[Boolean]("private_response")
  def maybeAuthorId = column[Option[String]]("author_id")
  def createdAt = column[DateTime]("created_at")

  def * =
    (id, behaviorId, maybeDescription, maybeShortName, maybeFunctionBody, maybeResponseTemplate, forcePrivateResponse, maybeAuthorId, createdAt) <>
      ((RawBehaviorVersion.apply _).tupled, RawBehaviorVersion.unapply _)
}

class BehaviorVersionServiceImpl @Inject() (
                                      dataServiceProvider: Provider[DataService],
                                      lambdaServiceProvider: Provider[AWSLambdaService]
                                    ) extends BehaviorVersionService {

  def dataService = dataServiceProvider.get
  def lambdaService = lambdaServiceProvider.get

  import BehaviorVersionQueries._

  def allOfThem: Future[Seq[BehaviorVersion]] = {
    val action = allWithBehavior.result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
    dataService.run(action)
  }

  def uncompiledCurrentWithFunctionQuery() = {
    allWithBehavior.
      filter { case((version, _), (behavior, team)) => behavior.maybeCurrentVersionId === version.id}.
      filter { case((version, _), _) => version.maybeFunctionBody.map(_.trim.length > 0).getOrElse(false) }.
      map { case((version, _), _) => version.id }
  }
  val currentWithFunctionQuery = Compiled(uncompiledCurrentWithFunctionQuery)

  def currentIdsWithFunction: Future[Seq[String]] = {
    dataService.run(uncompiledCurrentWithFunctionQuery.result)
  }

  def uncompiledAllForQuery(behaviorId: Rep[String]) = {
    allWithBehavior.
      filter { case((version, _), _) => version.behaviorId === behaviorId }.
      sortBy { case((version, _), _) => version.createdAt.desc }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(behavior: Behavior): Future[Seq[BehaviorVersion]] = {
    val action = allForQuery(behavior.id).result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
    dataService.run(action)
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithBehavior.filter { case((version, _), _) => version.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorVersion]] = {
    val action = findQuery(id).result.map(_.headOption.map(tuple2BehaviorVersion))
    dataService.run(action)
  }

  def createFor(behavior: Behavior, maybeUser: Option[User]): Future[BehaviorVersion] = {
    val raw = RawBehaviorVersion(IDs.next, behavior.id, None, None, None, None, forcePrivateResponse=false, maybeUser.map(_.id), DateTime.now)

    val action = (all += raw).map { _ =>
      BehaviorVersion(raw.id, behavior, raw.maybeDescription, raw.maybeShortName, raw.maybeFunctionBody, raw.maybeResponseTemplate, raw.forcePrivateResponse, maybeUser, raw.createdAt)
    }
    dataService.run(action)
  }

  def createFor(
                 behavior: Behavior,
                 maybeUser: Option[User],
                 data: BehaviorVersionData
               ): Future[BehaviorVersion] = {
    val action = (for {
      behaviorVersion <- DBIO.from(createFor(behavior, maybeUser))
      _ <-
      for {
        updated <- DBIO.from(save(behaviorVersion.copy(
          maybeFunctionBody = Some(data.functionBody),
          maybeResponseTemplate = Some(data.responseTemplate),
          forcePrivateResponse = data.config.forcePrivateResponse.exists(identity)
        )))
        maybeAWSConfig <- data.awsConfig.map { c =>
          DBIO.from(dataService.awsConfigs.createFor(updated, c.accessKeyName, c.secretKeyName, c.regionName)).map(Some(_))
        }.getOrElse(DBIO.successful(None))
        requiredOAuth2ApiConfigs <- DBIO.sequence(data.config.requiredOAuth2ApiConfigs.getOrElse(Seq()).map { requiredData =>
          DBIO.from(dataService.requiredOAuth2ApiConfigs.maybeCreateFor(requiredData, updated))
        }).map(_.flatten)
        _ <- DBIO.from(lambdaService.deployFunctionFor(
          updated,
          data.functionBody,
          withoutBuiltin(data.params.map(_.name).toArray),
          maybeAWSConfig,
          requiredOAuth2ApiConfigs
        ))
        _ <- DBIO.from(dataService.behaviorParameters.ensureFor(updated, data.params))
        _ <- DBIO.sequence(
          data.triggers.
            filterNot(_.text.trim.isEmpty)
            map { trigger =>
            DBIO.from(
              dataService.messageTriggers.createFor(
                updated,
                trigger.text,
                trigger.requiresMention,
                trigger.isRegex,
                trigger.caseSensitive
              )
            )
          }
        )
      } yield Unit
    } yield behaviorVersion) transactionally

    dataService.run(action)
  }

  def uncompiledFindQueryFor(id: Rep[String]) = all.filter(_.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def save(behaviorVersion: BehaviorVersion): Future[BehaviorVersion] = {
    val raw = behaviorVersion.toRaw
    val query = findQueryFor(raw.id)
    val action = query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse(all += raw)
    }.map(_ => behaviorVersion)
    dataService.run(action)
  }

  def delete(behaviorVersion: BehaviorVersion): Future[BehaviorVersion] = {
    val action = all.filter(_.id === behaviorVersion.id).delete.map(_ => behaviorVersion)
    dataService.run(action)
  }

  def unlearn(behaviorVersion: BehaviorVersion): Future[Unit] = {
    lambdaService.deleteFunction(behaviorVersion.id)
    delete(behaviorVersion).map(_ => Unit)
  }

  private def paramsIn(code: String): Array[String] = {
    """.*function\s*\(([^\)]*)\)""".r.findFirstMatchIn(code).flatMap { firstMatch =>
      firstMatch.subgroups.headOption.map { paramString =>
        paramString.split("""\s*,\s*""").filter(_.nonEmpty)
      }
    }.getOrElse(Array())
  }

  import services.AWSLambdaConstants._

  def withoutBuiltin(params: Array[String]) = params.filterNot(ea => ea == CONTEXT_PARAM)

  def environmentVariablesUsedInCode(functionBody: String): Seq[String] = {
    // regex quite incomplete, but we're just trying to provide some guidance
    """(?s)ellipsis\.env\.([$A-Za-z_][0-9A-Za-z_$]*)""".r.findAllMatchIn(functionBody).flatMap { m =>
      m.subgroups.headOption
    }.toSeq
  }

  private def environmentVariablesUsedInConfigFor(behaviorVersion: BehaviorVersion): Future[Seq[String]] = {
    dataService.awsConfigs.maybeFor(behaviorVersion).map { maybeAwsConfig =>
      maybeAwsConfig.map { awsConfig =>
        awsConfig.environmentVariableNames
      }.getOrElse(Seq())
    }
  }

  def knownEnvironmentVariablesUsedIn(behaviorVersion: BehaviorVersion, dataService: DataService): Future[Seq[String]] = {
    environmentVariablesUsedInConfigFor(behaviorVersion).map { inConfig =>
      inConfig ++ dataService.environmentVariables.lookForInCode(behaviorVersion.functionBody)
    }
  }

  def missingEnvironmentVariablesIn(
                                     behaviorVersion: BehaviorVersion,
                                     environmentVariables: Seq[EnvironmentVariable],
                                     dataService: DataService
                                   ): Future[Seq[String]] = {
    knownEnvironmentVariablesUsedIn(behaviorVersion, dataService).map{ used =>
      used diff environmentVariables.filter(_.value.trim.nonEmpty).map(_.name)
    }
  }

  def maybeFunctionFor(behaviorVersion: BehaviorVersion): Future[Option[String]] = {
    behaviorVersion.maybeFunctionBody.map { functionBody =>
      (for {
        params <- dataService.behaviorParameters.allFor(behaviorVersion)
        maybeAWSConfig <- dataService.awsConfigs.maybeFor(behaviorVersion)
        requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allFor(behaviorVersion)
      } yield {
        lambdaService.functionWithParams(params.map(_.name).toArray, functionBody)
      }).map(Some(_))
    }.getOrElse(Future.successful(None))
  }

  def maybePreviousFor(behaviorVersion: BehaviorVersion): Future[Option[BehaviorVersion]] = {
    allFor(behaviorVersion.behavior).map { versions =>
      val index = versions.indexWhere(_.id == behaviorVersion.id)
      if (index == versions.size - 1) {
        None
      } else {
        Some(versions(index + 1))
      }
    }
  }

  def resultFor(
                 behaviorVersion: BehaviorVersion,
                 parametersWithValues: Seq[ParameterWithValue],
                 event: MessageEvent
               ): Future[BotResult] = {
    for {
      envVars <- dataService.environmentVariables.allFor(behaviorVersion.team)
      result <- lambdaService.invoke(behaviorVersion, parametersWithValues, envVars, event)
    } yield result
  }

  def redeploy(behaviorVersion: BehaviorVersion): Future[Unit] = {
    for {
      params <- dataService.behaviorParameters.allFor(behaviorVersion)
      maybeAWSConfig <- dataService.awsConfigs.maybeFor(behaviorVersion)
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allFor(behaviorVersion)
      _ <- lambdaService.deployFunctionFor(
              behaviorVersion,
              behaviorVersion.functionBody,
              params.map(_.name).toArray,
              maybeAWSConfig,
              requiredOAuth2ApiConfigs
            )
    } yield {}
  }

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

  def resultFor(
                 behaviorVersion: BehaviorVersion,
                 payload: ByteBuffer,
                 logResult: AWSLambdaLogResult,
                 parametersWithValues: Seq[ParameterWithValue],
                 configuration: Configuration
               ): BotResult = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val json = Json.parse(jsonString)
    val logResultOption = Some(logResult)
    (json \ "result").toOption.map { successResult =>
      SuccessResult(successResult, parametersWithValues, behaviorVersion.maybeResponseTemplate, logResultOption, behaviorVersion.forcePrivateResponse)
    }.getOrElse {
      if ((json \ NO_RESPONSE_KEY).toOption.exists(_.as[Boolean])) {
        NoResponseResult(logResultOption)
      } else {
        if (isUnhandledError(json)) {
          UnhandledErrorResult(behaviorVersion, configuration, logResultOption)
        } else if (json.toString == "null") {
          NoCallbackTriggeredResult(behaviorVersion, configuration)
        } else if (isSyntaxError(json)) {
          SyntaxErrorResult(behaviorVersion, configuration, json, logResultOption)
        } else {
          HandledErrorResult(behaviorVersion, configuration, json, logResultOption)
        }
      }
    }
  }
}
