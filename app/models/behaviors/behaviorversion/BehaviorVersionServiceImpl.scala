package models.behaviors.behaviorversion

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.OffsetDateTime
import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Provider
import json.BehaviorVersionData
import models.IDs
import models.accounts.user.User
import models.behaviors._
import models.behaviors.behavior.{Behavior, BehaviorQueries}
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import services.{AWSLambdaLogResult, AWSLambdaService, DataService}
import drivers.SlickPostgresDriver.api._
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi
import models.behaviors.events.{Event, MessageEvent}
import models.team.Team

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RawBehaviorVersion(
                               id: String,
                               behaviorId: String,
                               groupVersionId: String,
                               maybeDescription: Option[String],
                               maybeName: Option[String],
                               maybeFunctionBody: Option[String],
                               maybeResponseTemplate: Option[String],
                               forcePrivateResponse: Boolean,
                               maybeAuthorId: Option[String],
                               createdAt: OffsetDateTime
                             )

class BehaviorVersionsTable(tag: Tag) extends Table[RawBehaviorVersion](tag, "behavior_versions") {

  def id = column[String]("id", O.PrimaryKey)
  def behaviorId = column[String]("behavior_id")
  def groupVersionId = column[String]("group_version_id")
  def maybeDescription = column[Option[String]]("description")
  def maybeName = column[Option[String]]("name")
  def maybeFunctionBody = column[Option[String]]("code")
  def maybeResponseTemplate = column[Option[String]]("response_template")
  def forcePrivateResponse = column[Boolean]("private_response")
  def maybeAuthorId = column[Option[String]]("author_id")
  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, behaviorId, groupVersionId, maybeDescription, maybeName, maybeFunctionBody, maybeResponseTemplate, forcePrivateResponse, maybeAuthorId, createdAt) <>
      ((RawBehaviorVersion.apply _).tupled, RawBehaviorVersion.unapply _)
}

class BehaviorVersionServiceImpl @Inject() (
                                      dataServiceProvider: Provider[DataService],
                                      lambdaServiceProvider: Provider[AWSLambdaService],
                                      ws: WSClient,
                                      configuration: Configuration,
                                      cache: CacheApi,
                                      implicit val actorSystem: ActorSystem
                                    ) extends BehaviorVersionService {

  def dataService = dataServiceProvider.get
  def lambdaService = lambdaServiceProvider.get

  import BehaviorVersionQueries._

  def allOfThem: Future[Seq[BehaviorVersion]] = {
    val action = allWithGroupVersion.result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
    dataService.run(action)
  }

  def uncompiledCurrentWithFunctionQuery() = {
    allWithGroupVersion.
      filter { case(_, ((groupVersion, (group, _)), _)) => group.maybeCurrentVersionId === groupVersion.id }.
      filter { case(((version, _), _), _) => version.maybeFunctionBody.map(_.trim.length > 0).getOrElse(false) }.
      map { case(((version, _), _), _) => version.id }
  }
  val currentWithFunctionQuery = Compiled(uncompiledCurrentWithFunctionQuery)

  def currentIdsWithFunction: Future[Seq[String]] = {
    dataService.run(uncompiledCurrentWithFunctionQuery.result)
  }

  def uncompiledAllForQuery(behaviorId: Rep[String]) = {
    allWithGroupVersion.
      filter { case(((version, _), _), _) => version.behaviorId === behaviorId }.
      sortBy { case(((version, _), _), _) => version.createdAt.desc }
  }
  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(behavior: Behavior): Future[Seq[BehaviorVersion]] = {
    val action = allForQuery(behavior.id).result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
    dataService.run(action)
  }

  def uncompiledAllForGroupVersionQuery(groupVersionId: Rep[String]) = {
    allWithGroupVersion.filter { case(_, ((groupVersion, _), _)) => groupVersion.id === groupVersionId }
  }
  val allForGroupVersionQuery = Compiled(uncompiledAllForGroupVersionQuery _)

  def allForGroupVersionAction(groupVersion: BehaviorGroupVersion): DBIO[Seq[BehaviorVersion]] = {
    allForGroupVersionQuery(groupVersion.id).result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
  }

  def allForGroupVersion(groupVersion: BehaviorGroupVersion): Future[Seq[BehaviorVersion]] = {
    dataService.run(allForGroupVersionAction(groupVersion))
  }

  def uncompiledAllForTeamQuery(teamId: Rep[String]) = {
    allWithGroupVersion.filter { case(_, ((_, (_, team)), _)) => team.id === teamId }
  }
  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allCurrentForTeam(team: Team): Future[Seq[BehaviorVersion]] = {
    val action = allForTeamQuery(team.id).result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
    dataService.run(action)
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithGroupVersion.filter { case(((version, _), _), _) => version.id === id }
  }
  val findQuery = Compiled(uncompiledFindQuery _)

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorVersion]] = {
    val action = findQuery(id).result.map(_.headOption.map(tuple2BehaviorVersion))
    dataService.run(action)
  }

  def uncompiledFindForBehaviorAndGroupVersionQuery(behaviorId: Rep[String], groupVersionId: Rep[String]) = {
    allWithGroupVersion.filter { case(((behaviorVersion, _), _), ((groupVersion, _), _)) => behaviorVersion.behaviorId === behaviorId && groupVersion.id === groupVersionId }
  }
  val findForBehaviorAndGroupVersionQuery = Compiled(uncompiledFindForBehaviorAndGroupVersionQuery _)

  def findForAction(behavior: Behavior, groupVersion: BehaviorGroupVersion): DBIO[Option[BehaviorVersion]] = {
    findForBehaviorAndGroupVersionQuery(behavior.id, groupVersion.id).result.map { r =>
      r.headOption.map(tuple2BehaviorVersion)
    }
  }

  def findFor(behavior: Behavior, groupVersion: BehaviorGroupVersion): Future[Option[BehaviorVersion]] = {
    dataService.run(findForAction(behavior, groupVersion))
  }

  def findCurrentByName(name: String, group: BehaviorGroup): Future[Option[BehaviorVersion]] = {
    val action = findCurrentByNameQuery(name, group.id).result.map { r =>
      r.headOption.map(tuple2BehaviorVersion)
    }
    dataService.run(action)
  }

  def hasSearchParam(behaviorVersion: BehaviorVersion): Future[Boolean] = {
    for {
      params <- dataService.behaviorParameters.allFor(behaviorVersion)
    } yield {
      params.exists(_.name == BehaviorQueries.SEARCH_QUERY_PARAM)
    }
  }

  def createForAction(
                       behavior: Behavior,
                       groupVersion: BehaviorGroupVersion,
                       maybeUser: Option[User],
                       maybeId: Option[String]
                     ): DBIO[BehaviorVersion] = {
    val raw = RawBehaviorVersion(
      maybeId.getOrElse(IDs.next),
      behavior.id,
      groupVersion.id,
      None,
      None,
      None,
      None,
      forcePrivateResponse=false,
      maybeUser.map(_.id),
      OffsetDateTime.now
    )

    (all += raw).map { _ =>
      BehaviorVersion(raw.id, behavior, groupVersion, raw.maybeDescription, raw.maybeName, raw.maybeFunctionBody, raw.maybeResponseTemplate, raw.forcePrivateResponse, maybeUser, raw.createdAt)
    }
  }

  def createForAction(
                       behavior: Behavior,
                       groupVersion: BehaviorGroupVersion,
                       requiredOAuth2ApiConfigs: Seq[RequiredOAuth2ApiConfig],
                       requiredSimpleTokenApis: Seq[RequiredSimpleTokenApi],
                       maybeUser: Option[User],
                       data: BehaviorVersionData
                     ): DBIO[BehaviorVersion] = {
    for {
      behaviorVersion <- createForAction(behavior, groupVersion, maybeUser, data.id)
      _ <-
      for {
        updated <- saveAction(behaviorVersion.copy(
          maybeName = data.name,
          maybeDescription = data.description,
          maybeFunctionBody = Some(data.functionBody),
          maybeResponseTemplate = Some(data.responseTemplate),
          forcePrivateResponse = data.config.forcePrivateResponse.exists(identity)
        ))
        maybeAWSConfig <- data.awsConfig.map { c =>
          dataService.awsConfigs.createForAction(updated, c.accessKeyName, c.secretKeyName, c.regionName).map(Some(_))
        }.getOrElse(DBIO.successful(None))
        inputs <- DBIO.sequence(data.inputIds.map { inputId =>
          dataService.inputs.findByInputIdAction(inputId)
        }).map(_.flatten)
        _ <- DBIO.from(lambdaService.deployFunctionFor(
          updated,
          data.functionBody,
          withoutBuiltin(inputs.map(_.name).toArray),
          maybeAWSConfig,
          requiredOAuth2ApiConfigs,
          requiredSimpleTokenApis
        ))
        _ <- dataService.behaviorParameters.ensureForAction(updated, inputs)
        _ <- DBIO.sequence(
          data.triggers.
            filterNot(_.text.trim.isEmpty)
            map { trigger =>
            dataService.messageTriggers.createForAction(
              updated,
              trigger.text,
              trigger.requiresMention,
              trigger.isRegex,
              trigger.caseSensitive
            )
          }
        )
      } yield Unit
    } yield behaviorVersion
  }

  def uncompiledFindQueryFor(id: Rep[String]) = all.filter(_.id === id)
  val findQueryFor = Compiled(uncompiledFindQueryFor _)

  def saveAction(behaviorVersion: BehaviorVersion): DBIO[BehaviorVersion] = {
    val raw = behaviorVersion.toRaw
    val query = findQueryFor(raw.id)
    query.result.flatMap { r =>
      r.headOption.map { existing =>
        query.update(raw)
      }.getOrElse(all += raw)
    }.map(_ => behaviorVersion)
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

  def maybeFunctionFor(behaviorVersion: BehaviorVersion): Future[Option[String]] = {
    behaviorVersion.maybeFunctionBody.map { functionBody =>
      (for {
        params <- dataService.behaviorParameters.allFor(behaviorVersion)
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

  def maybeNotReadyResultFor(behaviorVersion: BehaviorVersion, event: Event): Future[Option[BotResult]] = {
    for {
      missingTeamEnvVars <- dataService.teamEnvironmentVariables.missingIn(behaviorVersion, dataService)
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allFor(behaviorVersion.groupVersion)
      userInfo <- event.userInfo(ws, dataService)
      notReadyOAuth2Applications <- Future.successful(requiredOAuth2ApiConfigs.filterNot(_.isReady))
      missingOAuth2Applications <- Future.successful(requiredOAuth2ApiConfigs.flatMap(_.maybeApplication).filter { app =>
        !userInfo.links.exists(_.externalSystem == app.name)
      })
      maybeResult <- if (missingTeamEnvVars.nonEmpty) {
        Future.successful(Some(MissingTeamEnvVarsResult(event, behaviorVersion, dataService, configuration, missingTeamEnvVars)))
      } else {
        notReadyOAuth2Applications.headOption.map { firstNotReadyOAuth2App =>
          Future.successful(Some(RequiredApiNotReady(firstNotReadyOAuth2App, event, cache, dataService, configuration)))
        }.getOrElse {
          val missingOAuth2ApplicationsRequiringAuth = missingOAuth2Applications.filter(_.api.grantType.requiresAuth)
          missingOAuth2ApplicationsRequiringAuth.headOption.map { firstMissingOAuth2App =>
            event.ensureUser(dataService).flatMap { user =>
              dataService.loginTokens.createFor(user).map { loginToken =>
                OAuth2TokenMissing(firstMissingOAuth2App, event, loginToken, cache, configuration)
              }
            }.map(Some(_))
          }.getOrElse(Future.successful(None))
        }
      }
    } yield maybeResult
  }

  def resultFor(
                 behaviorVersion: BehaviorVersion,
                 parametersWithValues: Seq[ParameterWithValue],
                 event: Event
               ): Future[BotResult] = {
    for {
      teamEnvVars <- dataService.teamEnvironmentVariables.allFor(behaviorVersion.team)
      user <- event.ensureUser(dataService)
      userEnvVars <- dataService.userEnvironmentVariables.allFor(user)
      result <- maybeNotReadyResultFor(behaviorVersion, event).flatMap { maybeResult =>
        maybeResult.map(Future.successful).getOrElse {
          lambdaService.invoke(behaviorVersion, parametersWithValues, (teamEnvVars ++ userEnvVars), event)
        }
      }
    } yield result
  }

  def redeploy(behaviorVersion: BehaviorVersion): Future[Unit] = {
    val groupVersion = behaviorVersion.groupVersion
    for {
      params <- dataService.behaviorParameters.allFor(behaviorVersion)
      maybeAWSConfig <- dataService.awsConfigs.maybeFor(behaviorVersion)
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allFor(groupVersion)
      requiredSimpleTokenApis <- dataService.requiredSimpleTokenApis.allFor(groupVersion)
      _ <- lambdaService.deployFunctionFor(
              behaviorVersion,
              behaviorVersion.functionBody,
              params.map(_.name).toArray,
              maybeAWSConfig,
              requiredOAuth2ApiConfigs,
              requiredSimpleTokenApis
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
                 configuration: Configuration,
                 event: MessageEvent
               ): BotResult = {
    val bytes = payload.array
    val jsonString = new java.lang.String( bytes, Charset.forName("UTF-8") )
    val json = Json.parse(jsonString)
    val logResultOption = Some(logResult)
    (json \ "result").toOption.map { successResult =>
      SuccessResult(event, successResult, parametersWithValues, behaviorVersion.maybeResponseTemplate, logResultOption, behaviorVersion.forcePrivateResponse)
    }.getOrElse {
      if ((json \ NO_RESPONSE_KEY).toOption.exists(_.as[Boolean])) {
        NoResponseResult(event, logResultOption)
      } else {
        if (isUnhandledError(json)) {
          UnhandledErrorResult(event, behaviorVersion, dataService, configuration, logResultOption)
        } else if (json.toString == "null") {
          NoCallbackTriggeredResult(event, behaviorVersion, dataService, configuration)
        } else if (isSyntaxError(json)) {
          SyntaxErrorResult(event, behaviorVersion, dataService, configuration, json, logResultOption)
        } else {
          HandledErrorResult(event, behaviorVersion, dataService, configuration, json, logResultOption)
        }
      }
    }
  }
}
