package models.behaviors.behaviorversion

import java.time.OffsetDateTime
import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Provider
import drivers.SlickPostgresDriver.api._
import json.BehaviorVersionData
import models.IDs
import models.accounts.user.User
import models.behaviors._
import models.behaviors.behavior.{Behavior, BehaviorQueries}
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.config.requiredawsconfig.RequiredAWSConfig
import models.behaviors.config.requiredoauth2apiconfig.RequiredOAuth2ApiConfig
import models.behaviors.config.requiredsimpletokenapi.RequiredSimpleTokenApi
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.team.Team
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import services.{AWSLambdaService, ApiConfigInfo, CacheService, DataService}

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
                                      cacheService: CacheService,
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
      filter { case (_, ((groupVersion, (group, _)), _)) => group.maybeCurrentVersionId === groupVersion.id }.
      filter { case (((version, _), _), _) => version.maybeFunctionBody.map(_.trim.length > 0).getOrElse(false) }.
      map { case (((version, _), _), _) => version.id }
  }

  val currentWithFunctionQuery = Compiled(uncompiledCurrentWithFunctionQuery)

  def currentFunctionNames: Future[Seq[String]] = {
    dataService.run(uncompiledCurrentWithFunctionQuery.result).map { r =>
      r.map(BehaviorVersion.functionNameFor)
    }
  }

  def uncompiledAllForQuery(behaviorId: Rep[String]) = {
    allWithGroupVersion.
      filter { case (((version, _), _), _) => version.behaviorId === behaviorId }.
      sortBy { case (((version, _), _), _) => version.createdAt.desc }
  }

  val allForQuery = Compiled(uncompiledAllForQuery _)

  def allFor(behavior: Behavior): Future[Seq[BehaviorVersion]] = {
    val action = allForQuery(behavior.id).result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
    dataService.run(action)
  }

  def uncompiledAllForGroupVersionQuery(groupVersionId: Rep[String]) = {
    allWithGroupVersion.filter { case (_, ((groupVersion, _), _)) => groupVersion.id === groupVersionId }
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
    allWithGroupVersion.filter { case (_, ((_, (_, team)), _)) => team.id === teamId }
  }

  val allForTeamQuery = Compiled(uncompiledAllForTeamQuery _)

  def allCurrentForTeam(team: Team): Future[Seq[BehaviorVersion]] = {
    val action = allForTeamQuery(team.id).result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
    dataService.run(action)
  }

  def uncompiledFindQuery(id: Rep[String]) = {
    allWithGroupVersion.filter { case (((version, _), _), _) => version.id === id }
  }

  val findQuery = Compiled(uncompiledFindQuery _)

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorVersion]] = {
    val action = findQuery(id).result.map(_.headOption.map(tuple2BehaviorVersion))
    dataService.run(action)
  }

  def uncompiledFindForBehaviorAndGroupVersionQuery(behaviorId: Rep[String], groupVersionId: Rep[String]) = {
    allWithGroupVersion.filter { case (((behaviorVersion, _), _), ((groupVersion, _), _)) => behaviorVersion
      .behaviorId === behaviorId && groupVersion.id === groupVersionId
    }
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
    dataService.run(findCurrentByNameAction(name, group))
  }

  def findCurrentByNameAction(name: String, group: BehaviorGroup): DBIO[Option[BehaviorVersion]] = {
    findCurrentByNameQuery(name, group.id).result.map { r =>
      r.headOption.map(tuple2BehaviorVersion)
    }
  }

  def hasSearchParamAction(behaviorVersion: BehaviorVersion): DBIO[Boolean] = {
    for {
      params <- dataService.behaviorParameters.allForAction(behaviorVersion)
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
      forcePrivateResponse = false,
      maybeUser.map(_.id),
      OffsetDateTime.now
    )

    (all += raw).map { _ =>
      BehaviorVersion(
        raw.id,
        behavior,
        groupVersion,
        raw.maybeDescription,
        raw.maybeName,
        raw.maybeFunctionBody,
        raw.maybeResponseTemplate,
        raw.forcePrivateResponse,
        maybeUser,
        raw.createdAt
      )
    }
  }

  def createForAction(
                       behavior: Behavior,
                       groupVersion: BehaviorGroupVersion,
                       apiConfigInfo: ApiConfigInfo,
                       maybeUser: Option[User],
                       data: BehaviorVersionData,
                       forceNodeModuleUpdate: Boolean
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
        inputs <- DBIO.sequence(data.inputIds.map { inputId =>
          dataService.inputs.findByInputIdAction(inputId, groupVersion)
        }
        ).map(_.flatten)
        libraries <- dataService.libraries.allForAction(groupVersion)
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
        _ <- data.config.dataTypeConfig.map { configData =>
          dataService.dataTypeConfigs.createForAction(updated, configData)
        }.getOrElse(DBIO.successful(None))
        _ <- lambdaService.ensureNodeModuleVersionsFor(updated)
      } yield {
        // deploy in the background
        lambdaService.deployFunctionFor(
          updated,
          data.functionBody,
          withoutBuiltin(inputs.map(_.name).toArray),
          libraries,
          apiConfigInfo,
          forceNodeModuleUpdate
        )
      }
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

  def maybeNotReadyResultForAction(behaviorVersion: BehaviorVersion, event: Event): DBIO[Option[BotResult]] = {
    for {
      missingTeamEnvVars <- dataService.teamEnvironmentVariables.missingInAction(behaviorVersion, dataService)
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allForAction(behaviorVersion.groupVersion)
      userInfo <- event.userInfoAction(ws, dataService)
      notReadyOAuth2Applications <- DBIO.successful(requiredOAuth2ApiConfigs.filterNot(_.isReady))
      missingOAuth2Applications <- DBIO.successful(requiredOAuth2ApiConfigs.flatMap(_.maybeApplication).filter { app =>
        !userInfo.links.exists(_.externalSystem == app.name)
      })
      botPrefix <- DBIO.from(event.botPrefix(cacheService))
      maybeResult <- if (missingTeamEnvVars.nonEmpty) {
        DBIO.successful(Some(MissingTeamEnvVarsResult(
          event,
          None,
          behaviorVersion,
          dataService,
          configuration,
          missingTeamEnvVars,
          botPrefix
        )
        )
        )
      } else {
        notReadyOAuth2Applications.headOption.map { firstNotReadyOAuth2App =>
          DBIO.successful(Some(RequiredApiNotReady(firstNotReadyOAuth2App, event, None,  dataService, configuration)
          ))
        }.getOrElse {
          val missingOAuth2ApplicationsRequiringAuth = missingOAuth2Applications.filter(_.api.grantType.requiresAuth)
          missingOAuth2ApplicationsRequiringAuth.headOption.map { firstMissingOAuth2App =>
            event.ensureUserAction(dataService).flatMap { user =>
              dataService.loginTokens.createForAction(user).map { loginToken =>
                OAuth2TokenMissing(firstMissingOAuth2App, event, None, loginToken, cacheService, configuration)
              }
            }.map(Some(_))
          }.getOrElse(DBIO.successful(None))
        }
      }
    } yield maybeResult
  }

  def maybeNotReadyResultFor(behaviorVersion: BehaviorVersion, event: Event): Future[Option[BotResult]] = {
    dataService.run(maybeNotReadyResultForAction(behaviorVersion, event))
  }

  def resultForAction(
                       behaviorVersion: BehaviorVersion,
                       parametersWithValues: Seq[ParameterWithValue],
                       event: Event,
                       maybeConversation: Option[Conversation]
                     ): DBIO[BotResult] = {
    for {
      teamEnvVars <- dataService.teamEnvironmentVariables.allForAction(behaviorVersion.team)
      user <- event.ensureUserAction(dataService)
      userEnvVars <- dataService.userEnvironmentVariables.allForAction(user)
      result <- maybeNotReadyResultForAction(behaviorVersion, event).flatMap { maybeResult =>
        maybeResult.map(DBIO.successful).getOrElse {
          lambdaService
            .invokeAction(behaviorVersion, parametersWithValues, (teamEnvVars ++ userEnvVars), event, maybeConversation)
        }
      }
    } yield result
  }

  def resultFor(
                 behaviorVersion: BehaviorVersion,
                 parametersWithValues: Seq[ParameterWithValue],
                 event: Event,
                 maybeConversation: Option[Conversation]
               ): Future[BotResult] = {
    dataService.run(resultForAction(behaviorVersion, parametersWithValues, event, maybeConversation))
  }

  def redeploy(behaviorVersion: BehaviorVersion): Future[Unit] = {
    val groupVersion = behaviorVersion.groupVersion
    for {
      params <- dataService.behaviorParameters.allFor(behaviorVersion)
      awsConfigs <- dataService.awsConfigs.allFor(groupVersion.team)
      requiredAWSConfigs <- dataService.requiredAWSConfigs.allFor(groupVersion)
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allFor(groupVersion)
      requiredSimpleTokenApis <- dataService.requiredSimpleTokenApis.allFor(groupVersion)
      apiConfig <- Future.successful(ApiConfigInfo(awsConfigs, requiredAWSConfigs, requiredOAuth2ApiConfigs, requiredSimpleTokenApis))
      libraries <- dataService.libraries.allFor(behaviorVersion.groupVersion)
      _ <- lambdaService.deployFunctionFor(
        behaviorVersion,
        behaviorVersion.functionBody,
        params.map(_.name).toArray,
        libraries,
        apiConfig,
        forceNodeModuleUpdate = true
      )
    } yield {}
  }

  private def allCurrent: Future[Seq[BehaviorVersion]] = {
    val action = allWithGroupVersion.filter {
      case (_, ((groupVersion, (group, _)), _)) => groupVersion.id === group.maybeCurrentVersionId
    }.result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
    dataService.run(action)
  }

  private def redeployAllSequentially(versions: Seq[BehaviorVersion]): Future[Unit] = {
    versions.headOption.map { v =>
      redeploy(v).recover {
        case e: Exception => {
          Logger.info(s"Error redeploying version with ID: ${v.id}: ${e.getMessage}")
        }
      }.flatMap { _ =>
        redeployAllSequentially(versions.tail)
      }
    }.getOrElse(Future.successful(Unit))
  }

  def redeployAllCurrentVersions: Future[Unit] = {
    for {
      currentVersions <- allCurrent
      _ <- redeployAllSequentially(currentVersions)
    } yield {}
  }

}
