package models.behaviors.ellipsisobject

import models.behaviors.config.awsconfig.AWSConfig
import models.behaviors.config.requiredawsconfig.RequiredAWSConfig
import models.team.Team
import play.api.libs.json._
import play.api.libs.ws.WSClient
import services.ApiConfigInfo

import scala.concurrent.{ExecutionContext, Future}

case class TeamInfo(team: Team, links: Seq[IdentityInfo], requiredAWSConfigs: Seq[RequiredAWSConfig], maybeBotInfo: Option[BotInfo]) {

  val configuredRequiredAWSConfigs: Seq[(RequiredAWSConfig, AWSConfig)] = {
    requiredAWSConfigs.flatMap { ea =>
      ea.maybeConfig.map { cfg => (ea, cfg) }
    }
  }

  def toJson: JsObject = {
    val linkParts: Seq[(String, JsValue)] = Seq(
      "links" -> JsArray(links.map(_.toJson)),
      "aws" -> JsObject(configuredRequiredAWSConfigs.map { case(required, cfg) =>
        required.nameInCode -> JsObject(Seq(
          "accessKeyId" -> JsString(cfg.accessKey),
          "secretAccessKey" -> JsString(cfg.secretKey),
          "region" -> JsString(cfg.region)
        ))
      })
    )
    val botParts: Seq[(String, JsValue)] = maybeBotInfo.map { info =>
      Seq(
        "botName" -> JsString(info.name),
        "botUserIdForContext" -> JsString(info.userIdForContext)
      )
    }.getOrElse(Seq())
    val timeZonePart = Seq("timeZone" -> JsString(team.timeZone.toString))
    JsObject(linkParts ++ timeZonePart ++ botParts)
  }

}

object TeamInfo {

  def forConfig(apiConfigInfo: ApiConfigInfo, userInfo: DeprecatedUserInfo, team: Team, maybeBotInfo: Option[BotInfo], ws: WSClient)
               (implicit ec: ExecutionContext): Future[TeamInfo] = {
    val oauth2ApplicationsNeedingRefresh =
      apiConfigInfo.requiredOAuth2ApiConfigs.flatMap(_.maybeApplication).
        filter { app =>
          !userInfo.links.exists(_.platformName == app.name)
        }.
        filterNot(_.api.grantType.requiresAuth)
    val apps = oauth2ApplicationsNeedingRefresh
    Future.sequence(apps.map { ea =>
      ea.getClientCredentialsTokenFor(ws).map { maybeToken =>
        IdentityInfo(ea.api.name, Some(ea.name), None, maybeToken)
      }
    }).map { links =>
      TeamInfo(team, links, apiConfigInfo.requiredAWSConfigs, maybeBotInfo)
    }
  }

}
