package models.behaviors.behaviorversion

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import drivers.SlickPostgresDriver.api._
import javax.inject.Inject
import json.BehaviorVersionData
import models.IDs
import models.accounts.user.User
import models.behaviors._
import models.behaviors.behavior.{Behavior, BehaviorQueries}
import models.behaviors.behaviorgroup.BehaviorGroup
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.Event
import models.team.Team
import play.api.Configuration
import services._
import services.caching.CacheService

import scala.concurrent.{ExecutionContext, Future}

case class RawBehaviorVersion(
                               id: String,
                               behaviorId: String,
                               groupVersionId: String,
                               maybeDescription: Option[String],
                               maybeName: Option[String],
                               maybeFunctionBody: Option[String],
                               maybeResponseTemplate: Option[String],
                               forcePrivateResponse: Boolean,
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


  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, behaviorId, groupVersionId, maybeDescription, maybeName, maybeFunctionBody, maybeResponseTemplate, forcePrivateResponse, createdAt) <>
      ((RawBehaviorVersion.apply _).tupled, RawBehaviorVersion.unapply _)
}

class BehaviorVersionServiceImpl @Inject() (
                                      defaultServices: DefaultServices,
                                      configuration: Configuration,
                                      cacheService: CacheService,
                                      implicit val actorSystem: ActorSystem,
                                      implicit val ec: ExecutionContext
                                    ) extends BehaviorVersionService {

  def dataService = defaultServices.dataService
  def lambdaService = defaultServices.lambdaService

  import BehaviorVersionQueries._

  def allOfThem: Future[Seq[BehaviorVersion]] = {
    val action = allWithGroupVersion.result.map { r =>
      r.map(tuple2BehaviorVersion)
    }
    dataService.run(action)
  }

  def uncompiledAllForQuery(behaviorId: Rep[String]) = {
    allWithGroupVersion.
      filter { case ((version, _), _) => version.behaviorId === behaviorId }.
      sortBy { case ((version, _), _) => version.createdAt.desc }
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
    allWithGroupVersion.filter { case ((version, _), _) => version.id === id }
  }

  val findQuery = Compiled(uncompiledFindQuery _)

  def findWithoutAccessCheck(id: String): Future[Option[BehaviorVersion]] = {
    val action = findQuery(id).result.map(_.headOption.map(tuple2BehaviorVersion))
    dataService.run(action)
  }

  def uncompiledFindForBehaviorAndGroupVersionQuery(behaviorId: Rep[String], groupVersionId: Rep[String]) = {
    allWithGroupVersion.filter { case ((behaviorVersion, _), ((groupVersion, _), _)) => behaviorVersion
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

  def findByName(name: String, groupVersion: BehaviorGroupVersion): Future[Option[BehaviorVersion]] = {
    val action = findWithNameQuery(name, groupVersion.id).result.map { r =>
      r.headOption.map(tuple2BehaviorVersion)
    }
    dataService.run(action)
  }

  def findCurrentByName(name: String, group: BehaviorGroup): Future[Option[BehaviorVersion]] = {
    dataService.run(findCurrentByNameAction(name, group))
  }

  def findCurrentByNameAction(name: String, group: BehaviorGroup): DBIO[Option[BehaviorVersion]] = {
    dataService.behaviorGroupVersions.maybeCurrentForAction(group).flatMap { maybeGroupVersion =>
      maybeGroupVersion.map { groupVersion =>
        findWithNameQuery(name, groupVersion.id).result.map { r =>
          r.headOption.map(tuple2BehaviorVersion)
        }
      }.getOrElse(DBIO.successful(None))
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
      OffsetDateTime.now
    )

    (all += raw).map { _ =>
      BehaviorVersion(
        raw.id,
        behavior,
        groupVersion,
        raw.maybeDescription,
        raw.maybeName,
        raw.maybeFunctionBody.map(_.trim),
        raw.maybeResponseTemplate,
        raw.forcePrivateResponse,
        raw.createdAt
      )
    }
  }

  def createForAction(
                       behavior: Behavior,
                       groupVersion: BehaviorGroupVersion,
                       apiConfigInfo: ApiConfigInfo,
                       maybeUser: Option[User],
                       data: BehaviorVersionData
                     ): DBIO[BehaviorVersion] = {
    for {
      behaviorVersion <- createForAction(behavior, groupVersion, maybeUser, data.id)
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

  def maybeFunctionFor(behaviorVersion: BehaviorVersion): Future[Option[String]] = {
    behaviorVersion.maybeFunctionBody.map { functionBody =>
      (for {
        params <- dataService.behaviorParameters.allFor(behaviorVersion)
      } yield {
        lambdaService.functionWithParams(params, functionBody, isForExport = true)
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
      userInfo <- event.userInfoAction(defaultServices)
      notReadyOAuth2Applications <- DBIO.successful(requiredOAuth2ApiConfigs.filterNot(_.isReady))
      missingOAuth2Applications <- DBIO.successful(requiredOAuth2ApiConfigs.flatMap(_.maybeApplication).filter { app =>
        !userInfo.links.exists(_.externalSystem == app.name)
      })
      botPrefix <- DBIO.from(event.contextualBotPrefix(defaultServices))
      isForUndeployed <- dataService.behaviorGroupDeployments.findForBehaviorGroupVersionAction(behaviorVersion.groupVersion).map(_.isEmpty)
      user <- event.ensureUserAction(dataService)
      hasUndeployedVersionForAuthor <- dataService.behaviorGroupDeployments.hasUndeployedVersionForAuthorAction(behaviorVersion.groupVersion, user)
      isInDevMode <- event.maybeChannel.map { channel =>
        dataService.devModeChannels.isEnabledForAction(event.context, channel, behaviorVersion.team)
      }.getOrElse(DBIO.successful(false))
      maybeResult <- if (missingTeamEnvVars.nonEmpty) {
        DBIO.successful(Some(MissingTeamEnvVarsResult(
          event,
          None,
          behaviorVersion,
          dataService,
          configuration,
          missingTeamEnvVars,
          botPrefix,
          isForUndeployed,
          hasUndeployedVersionForAuthor,
          isInDevMode
        )
        )
        )
      } else {
        notReadyOAuth2Applications.headOption.map { firstNotReadyOAuth2App =>
          DBIO.successful(Some(RequiredApiNotReady(firstNotReadyOAuth2App, event, behaviorVersion, None,  dataService, configuration, isForUndeployed, hasUndeployedVersionForAuthor, isInDevMode)
          ))
        }.getOrElse {
          val missingOAuth2ApplicationsRequiringAuth = missingOAuth2Applications.filter(_.api.grantType.requiresAuth)
          missingOAuth2ApplicationsRequiringAuth.headOption.map { firstMissingOAuth2App =>
            event.ensureUserAction(dataService).flatMap { user =>
              dataService.loginTokens.createForAction(user).map { loginToken =>
                OAuth2TokenMissing(firstMissingOAuth2App, event, behaviorVersion, None, loginToken, cacheService, configuration, isForUndeployed, hasUndeployedVersionForAuthor, isInDevMode)
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
      result <- maybeNotReadyResultForAction(behaviorVersion, event).flatMap { maybeResult =>
        maybeResult.map(DBIO.successful).getOrElse {
          lambdaService
            .invokeAction(behaviorVersion, parametersWithValues, teamEnvVars, event, maybeConversation, defaultServices)
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

}
