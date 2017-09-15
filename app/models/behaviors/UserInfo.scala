package models.behaviors

import akka.actor.ActorSystem
import models.accounts.user.User
import models.behaviors.config.awsconfig.AWSConfig
import models.behaviors.config.requiredawsconfig.RequiredAWSConfig
import models.behaviors.events.Event
import models.team.Team
import play.api.libs.ws.WSClient
import play.api.libs.json._
import services.{ApiConfigInfo, CacheService, DataService}
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class LinkedInfo(externalSystem: String, accessToken: String) {

  def toJson: JsObject = {
    JsObject(Seq(
      "externalSystem" -> JsString(externalSystem),
      "token" -> JsString(accessToken),
      "oauthToken" -> JsString(accessToken)
    ))
  }

}

case class MessageInfo(medium: String, channel: Option[String], userId: String, details: JsObject)

object MessageInfo {

  def buildFor(
                event: Event,
                ws: WSClient,
                dataService: DataService,
                cacheService: CacheService
              )(implicit actorSystem: ActorSystem, ec: ExecutionContext): Future[MessageInfo] = {
    event.detailsFor(ws, cacheService).map { details =>
      MessageInfo(event.name, event.maybeChannel, event.userIdForContext, details)
    }
  }

}

case class UserInfo(
                     user: User,
                     links: Seq[LinkedInfo],
                     maybeMessageInfo: Option[MessageInfo]
                   ) {

  implicit val messageInfoWrites = Json.writes[MessageInfo]

  def toJson: JsObject = {
    val linkParts: Seq[(String, JsValue)] = Seq(
      "links" -> JsArray(links.map(_.toJson))
    )
    val messageInfoPart = maybeMessageInfo.map { info =>
      Seq("messageInfo" -> Json.toJson(info))
    }.getOrElse(Seq())
    val userParts = Seq("ellipsisUserId" -> JsString(user.id))
    JsObject(userParts ++ linkParts ++ messageInfoPart)
  }
}

object UserInfo {

  def buildForAction(
                      user: User,
                      event: Event,
                      ws: WSClient,
                      dataService: DataService,
                      cacheService: CacheService
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[UserInfo] = {
    for {
      linkedOAuth2Tokens <- dataService.linkedOAuth2Tokens.allForUserAction(user, ws)
      linkedSimpleTokens <- dataService.linkedSimpleTokens.allForUserAction(user)
      links <- DBIO.successful {
        linkedOAuth2Tokens.map { ea =>
          LinkedInfo(ea.application.name, ea.accessToken)
        } ++ linkedSimpleTokens.map { ea =>
          LinkedInfo(ea.api.name, ea.accessToken)
        }
      }
      messageInfo <- DBIO.from(event.messageInfo(ws, dataService, cacheService))
    } yield {
      UserInfo(user, links, Some(messageInfo))
    }
  }

  def buildForAction(
                      event: Event,
                      teamId: String,
                      ws: WSClient,
                      dataService: DataService,
                      cacheService: CacheService
                    )(implicit actorSystem: ActorSystem, ec: ExecutionContext): DBIO[UserInfo] = {
    for {
      user <- event.ensureUserAction(dataService)
      info <- buildForAction(user, event, ws, dataService, cacheService)
    } yield info
  }

}

case class TeamInfo(team: Team, links: Seq[LinkedInfo], requiredAWSConfigs: Seq[RequiredAWSConfig]) {

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
    val timeZonePart = Seq("timeZone" -> JsString(team.timeZone.toString))
    JsObject(linkParts ++ timeZonePart)
  }

}

object TeamInfo {

  def forConfig(apiConfigInfo: ApiConfigInfo, userInfo: UserInfo, team: Team, ws: WSClient)(implicit ec: ExecutionContext): Future[TeamInfo] = {
    val oauth2ApplicationsNeedingRefresh =
      apiConfigInfo.requiredOAuth2ApiConfigs.flatMap(_.maybeApplication).
        filter { app =>
          !userInfo.links.exists(_.externalSystem == app.name)
        }.
        filterNot(_.api.grantType.requiresAuth)
    val apps = oauth2ApplicationsNeedingRefresh
    Future.sequence(apps.map { ea =>
      ea.getClientCredentialsTokenFor(ws).map { maybeToken =>
        maybeToken.map { token =>
          LinkedInfo(ea.name, token)
        }
      }
    }).map { linkMaybes =>
      TeamInfo(team, linkMaybes.flatten, apiConfigInfo.requiredAWSConfigs)
    }
  }

}
