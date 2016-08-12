package json

import models.Team
import models.accounts.User
import models.bots.config.{RequiredOAuth2ApplicationQueries, AWSConfigQueries}
import models.bots.triggers.MessageTriggerQueries
import models.bots.{BehaviorVersionQueries, BehaviorParameterQueries, BehaviorQueries}
import org.joda.time.DateTime
import play.api.libs.json.Json
import slick.dbio.DBIO
import scala.concurrent.ExecutionContext.Implicits.global
import Formatting._

case class BehaviorVersionData(
                                teamId: String,
                                behaviorId: Option[String],
                                functionBody: String,
                                responseTemplate: String,
                                params: Seq[BehaviorParameterData],
                                triggers: Seq[BehaviorTriggerData],
                                config: BehaviorConfig,
                                importedId: Option[String],
                                githubUrl: Option[String],
                                knownEnvVarsUsed: Seq[String],
                                createdAt: Option[DateTime]
                                ) {
  val awsConfig: Option[AWSConfigData] = config.aws

  def copyForTeam(team: Team): BehaviorVersionData = {
    copy(teamId = team.id)
  }
}

object BehaviorVersionData {

  private def extractFunctionBodyFrom(function: String): String = {
    """(?s)^\s*function\s*\([^\)]*\)\s*\{\s*(.*)\s*\}\s*$""".r.findFirstMatchIn(function).flatMap { m =>
      m.subgroups.headOption
    }.getOrElse("")
  }

  def buildFor(teamId: String,
            behaviorId: Option[String],
            functionBody: String,
            responseTemplate: String,
            params: Seq[BehaviorParameterData],
            triggers: Seq[BehaviorTriggerData],
            config: BehaviorConfig,
            importedId: Option[String],
            githubUrl: Option[String],
            createdAt: Option[DateTime]): BehaviorVersionData = {

    val knownEnvVarsUsed = config.knownEnvVarsUsed ++ BehaviorVersionQueries.environmentVariablesUsedInCode(functionBody)

    BehaviorVersionData(
    teamId,
    behaviorId,
    functionBody,
    responseTemplate,
    params,
    triggers,
    config,
    importedId,
    githubUrl,
    knownEnvVarsUsed,
    createdAt
    )
  }

  def fromStrings(
                   teamId: String,
                   function: String,
                   response: String,
                   params: String,
                   triggers: String,
                   config: String,
                   maybeGithubUrl: Option[String]
                   ): BehaviorVersionData = {
    BehaviorVersionData.buildFor(
      teamId,
      None,
      extractFunctionBodyFrom(function),
      response,
      Json.parse(params).validate[Seq[BehaviorParameterData]].get,
      Json.parse(triggers).validate[Seq[BehaviorTriggerData]].get,
      Json.parse(config).validate[BehaviorConfig].get,
      importedId = None,
      maybeGithubUrl,
      createdAt = None
    )
  }

  def maybeFor(behaviorId: String, user: User, maybePublishedId: Option[String] = None): DBIO[Option[BehaviorVersionData]] = {
    for {
      maybeBehavior <- BehaviorQueries.find(behaviorId, user)
      maybeBehaviorVersion <- maybeBehavior.map { behavior =>
        behavior.maybeCurrentVersion
      }.getOrElse(DBIO.successful(None))
      maybeParameters <- maybeBehaviorVersion.map { behaviorVersion =>
        BehaviorParameterQueries.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeTriggers <- maybeBehaviorVersion.map { behaviorVersion =>
        MessageTriggerQueries.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeAWSConfig <- maybeBehaviorVersion.map { behaviorVersion =>
        AWSConfigQueries.maybeFor(behaviorVersion)
      }.getOrElse(DBIO.successful(None))
      maybeRequiredOAuth2Applications <- maybeBehaviorVersion.map { behaviorVersion =>
        RequiredOAuth2ApplicationQueries.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
      for {
        behavior <- maybeBehavior
        behaviorVersion <- maybeBehaviorVersion
        params <- maybeParameters
        triggers <- maybeTriggers
        requiredOAuth2Applications <- maybeRequiredOAuth2Applications
      } yield {
        val maybeAWSConfigData = maybeAWSConfig.map { config =>
          AWSConfigData(config.maybeAccessKeyName, config.maybeSecretKeyName, config.maybeRegionName)
        }
        val maybeOAuth2ApplicationData = maybeRequiredOAuth2Applications.map { requiredOAuth2Applications =>
          requiredOAuth2Applications.map(ea => OAuth2ApplicationData.from(ea.application))
        }
        BehaviorVersionData.buildFor(
          behaviorVersion.team.id,
          Some(behavior.id),
          behaviorVersion.functionBody,
          behaviorVersion.maybeResponseTemplate.getOrElse(""),
          params.map { ea =>
            BehaviorParameterData(ea.name, ea.question)
          },
          triggers.map(ea =>
            BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
          ),
          BehaviorConfig(maybePublishedId, maybeAWSConfigData, maybeOAuth2ApplicationData),
          behavior.maybeImportedId,
          githubUrl = None,
          Some(behaviorVersion.createdAt)
        )
      }
    }
  }
}
