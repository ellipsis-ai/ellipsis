package models.behaviors.ellipsisobject

import models.behaviors.config.awsconfig.AWSConfig
import models.behaviors.config.requiredawsconfig.RequiredAWSConfig
import models.team.Team
import play.api.libs.ws.WSClient
import services.ApiConfigInfo

import scala.concurrent.{ExecutionContext, Future}

case class AWSConfigInfo(
                          accessKeyId: String,
                          secretAccessKey: String,
                          region: String
                        )

case class TeamInfo(
                     ellipsisTeamId: String,
                     links: Seq[IdentityInfo],
                     aws: Map[String, AWSConfigInfo],
                     botName: Option[String],
                     botUserIdForContext: Option[String],
                     timeZone: Option[String]
                   )

object TeamInfo {

  def forConfig(apiConfigInfo: ApiConfigInfo, userInfo: DeprecatedUserInfo, team: Team, maybeBotInfo: Option[BotInfo], ws: WSClient)
               (implicit ec: ExecutionContext): Future[TeamInfo] = {
    val configuredRequiredAWSConfigs: Seq[(RequiredAWSConfig, AWSConfig)] = {
      apiConfigInfo.requiredAWSConfigs.flatMap { ea =>
        ea.maybeConfig.map { cfg => (ea, cfg) }
      }
    }
    val aws = configuredRequiredAWSConfigs.map { case (required, cfg) =>
      required.nameInCode -> AWSConfigInfo(
        cfg.accessKey,
        cfg.secretKey,
        cfg.region
      )
    }.toMap
    val oauth2ApplicationsNeedingRefresh =
      apiConfigInfo.requiredOAuth2ApiConfigs.flatMap(_.maybeApplication).
        filter { app =>
          !userInfo.links.exists(_.externalSystem == app.name)
        }.
        filterNot(_.api.grantType.requiresAuth)
    val apps = oauth2ApplicationsNeedingRefresh
    Future.sequence(apps.map { ea =>
      ea.getClientCredentialsTokenFor(ws).map { maybeToken =>
        IdentityInfo.buildFor(ea.api.name, Some(ea.name), None, maybeToken)
      }
    }).map { links =>
      TeamInfo(
        team.id,
        links,
        aws,
        maybeBotInfo.map(_.name),
        maybeBotInfo.map(_.userIdForContext),
        Some(team.timeZone.toString)
      )
    }
  }

}
