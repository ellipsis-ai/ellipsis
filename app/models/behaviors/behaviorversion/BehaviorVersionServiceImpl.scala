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
import models.behaviors.triggers.TriggerType
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
                               responseType: BehaviorResponseType,
                               canBeMemoized: Boolean,
                               isTest: Boolean,
                               createdAt: OffsetDateTime
                             )

class BehaviorVersionsTable(tag: Tag) extends Table[RawBehaviorVersion](tag, "behavior_versions") {

  implicit val responseTypeColumnType = MappedColumnType.base[BehaviorResponseType, String](
    { gt => gt.toString },
    { str => BehaviorResponseType.definitelyFind(str) }
  )

  def id = column[String]("id", O.PrimaryKey)

  def behaviorId = column[String]("behavior_id")

  def groupVersionId = column[String]("group_version_id")

  def maybeDescription = column[Option[String]]("description")

  def maybeName = column[Option[String]]("name")

  def maybeFunctionBody = column[Option[String]]("code")

  def maybeResponseTemplate = column[Option[String]]("response_template")

  def responseType = column[BehaviorResponseType]("response_type")

  def canBeMemoized = column[Boolean]("can_be_memoized")

  def isTest = column[Boolean]("is_test")

  def createdAt = column[OffsetDateTime]("created_at")

  def * =
    (id, behaviorId, groupVersionId, maybeDescription, maybeName, maybeFunctionBody, maybeResponseTemplate, responseType, canBeMemoized, isTest, createdAt) <>
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

  def find(id: String, user: User): Future[Option[BehaviorVersion]] = {
    for {
      maybeBehaviorVersion <- findWithoutAccessCheck(id)
      canAccess <- maybeBehaviorVersion.map { behaviorVersion =>
        dataService.users.canAccess(user, behaviorVersion.behavior)
      }.getOrElse(Future.successful(false))
    } yield {
      if (canAccess) {
        maybeBehaviorVersion
      } else {
        None
      }
    }
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

  def findByNameAction(name: String, groupVersion: BehaviorGroupVersion): DBIO[Option[BehaviorVersion]] = {
    findWithNameQuery(name, groupVersion.id).result.map { r =>
      r.headOption.map(tuple2BehaviorVersion)
    }
  }

  def findByName(name: String, groupVersion: BehaviorGroupVersion): Future[Option[BehaviorVersion]] = {
    dataService.run(findByNameAction(name, groupVersion))
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

  private def haveSameName(behaviorVersion1: BehaviorVersion, behaviorVersion2: BehaviorVersion): Boolean = {
    behaviorVersion1.maybeName.exists(n => behaviorVersion2.maybeName.contains(n))
  }

  def haveSameInterface(behaviorVersion1: BehaviorVersion, behaviorVersion2: BehaviorVersion): Future[Boolean] = {
    if (!haveSameName(behaviorVersion1, behaviorVersion2)) {
      Future.successful(false)
    } else {
      for {
        params1 <- dataService.behaviorParameters.allFor(behaviorVersion1)
        params2 <- dataService.behaviorParameters.allFor(behaviorVersion2)
        paramsMatch <- if (params1.length != params2.length) {
          Future.successful(false)
        } else {
          Future.sequence(params1.zip(params2).map { case(p1, p2) =>
            dataService.behaviorParameters.haveSameInterface(p1, p2)
          }).map(matchResults => matchResults.forall(identity))
        }
      } yield paramsMatch
    }
  }

  def createForAction(
                       behavior: Behavior,
                       groupVersion: BehaviorGroupVersion,
                       maybeUser: Option[User],
                       maybeId: Option[String],
                       isTest: Boolean
                     ): DBIO[BehaviorVersion] = {
    val raw = RawBehaviorVersion(
      maybeId.getOrElse(IDs.next),
      behavior.id,
      groupVersion.id,
      None,
      None,
      None,
      None,
      responseType = Normal,
      canBeMemoized = false,
      isTest,
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
        raw.responseType,
        raw.canBeMemoized,
        raw.isTest,
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
      behaviorVersion <- createForAction(behavior, groupVersion, maybeUser, data.id, data.config.isTest.exists(identity))
      updated <- saveAction(behaviorVersion.copy(
        maybeName = data.name,
        maybeDescription = data.description,
        maybeFunctionBody = Some(data.functionBody),
        maybeResponseTemplate = Some(data.responseTemplate),
        responseType = BehaviorResponseType.definitelyFind(data.config.responseTypeId),
        canBeMemoized = data.config.canBeMemoized.exists(identity)
      ))
      inputs <- DBIO.sequence(data.inputIds.map { inputId =>
        dataService.inputs.findByInputIdAction(inputId, groupVersion)
      }
      ).map(_.flatten)
      _ <- dataService.behaviorParameters.ensureForAction(updated, inputs)
      _ <- dataService.triggers.createTriggersForAction(updated, data.triggers)
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
      requiredOAuth1ApiConfigs <- dataService.requiredOAuth1ApiConfigs.allForAction(behaviorVersion.groupVersion)
      requiredOAuth2ApiConfigs <- dataService.requiredOAuth2ApiConfigs.allForAction(behaviorVersion.groupVersion)
      userInfo <- event.deprecatedUserInfoAction(None, defaultServices)
      notReadyOAuth1Applications <- DBIO.successful(requiredOAuth1ApiConfigs.filterNot(_.isReady))
      notReadyOAuth2Applications <- DBIO.successful(requiredOAuth2ApiConfigs.filterNot(_.isReady))
      missingOAuth1Applications <- DBIO.successful(requiredOAuth1ApiConfigs.flatMap(_.maybeApplication).filter { app =>
        !userInfo.links.flatMap(_.integration).contains(app.name)
      })
      missingOAuth2Applications <- DBIO.successful(requiredOAuth2ApiConfigs.flatMap(_.maybeApplication).filter { app =>
        !userInfo.links.flatMap(_.integration).contains(app.name)
      })
      botPrefix <- DBIO.from(event.contextualBotPrefix(defaultServices))
      developerContext <- DeveloperContext.buildFor(event, behaviorVersion, dataService)
      maybeResult <- if (missingTeamEnvVars.nonEmpty) {
        DBIO.successful(Some(MissingTeamEnvVarsResult(
          event,
          None,
          behaviorVersion,
          dataService,
          configuration,
          missingTeamEnvVars,
          botPrefix,
          developerContext
        )
        )
        )
      } else {
        notReadyOAuth1Applications.headOption.map { firstNotReadyOAuth1App =>
          DBIO.successful(Some(RequiredOAuth1ApiNotReady(firstNotReadyOAuth1App, event, behaviorVersion, None,  dataService, configuration, developerContext)))
        }.getOrElse {
          notReadyOAuth2Applications.headOption.map { firstNotReadyOAuth2App =>
            DBIO.successful(Some(RequiredOAuth2ApiNotReady(firstNotReadyOAuth2App, event, behaviorVersion, None,  dataService, configuration, developerContext)))
          }.getOrElse {
            missingOAuth1Applications.headOption.map { firstMissingOAuth1App =>
              event.ensureUserAction(dataService).flatMap { user =>
                dataService.loginTokens.createForAction(user).map { loginToken =>
                  OAuth1TokenMissing(firstMissingOAuth1App, event, behaviorVersion, None, loginToken, cacheService, configuration, developerContext)
                }
              }.map(Some(_))
            }.getOrElse {
              val missingOAuth2ApplicationsRequiringAuth = missingOAuth2Applications.filter(_.api.grantType.requiresAuth)
              missingOAuth2ApplicationsRequiringAuth.headOption.map { firstMissingOAuth2App =>
                event.ensureUserAction(dataService).flatMap { user =>
                  dataService.loginTokens.createForAction(user).map { loginToken =>
                    OAuth2TokenMissing(firstMissingOAuth2App, event, behaviorVersion, None, loginToken, cacheService, configuration, developerContext)
                  }
                }.map(Some(_))
              }.getOrElse(DBIO.successful(None))
            }
          }
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
                     )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[BotResult] = {
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
               )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[BotResult] = {
    dataService.run(resultForAction(behaviorVersion, parametersWithValues, event, maybeConversation))
  }

}
