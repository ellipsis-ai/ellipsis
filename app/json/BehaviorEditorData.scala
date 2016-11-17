package json

import models.accounts.user.{User, UserTeamAccess}

import scala.concurrent.ExecutionContext.Implicits.global
import models.behaviors.behaviorparameter.BehaviorParameterType
import models.team.Team
import play.api.libs.ws.WSClient
import services.DataService

import scala.concurrent.Future

case class BehaviorEditorData(
                               teamAccess: UserTeamAccess,
                               behaviorVersion: BehaviorVersionData,
                               environmentVariables: Seq[EnvironmentVariableData],
                               paramTypes: Seq[BehaviorParameterTypeData],
                               oauth2Applications: Seq[OAuth2ApplicationData],
                               oauth2Apis: Seq[OAuth2ApiData],
                               simpleTokenApis: Seq[SimpleTokenApiData],
                               linkedOAuth2ApplicationIds: Seq[String],
                               justSaved: Boolean
                              ) {

  def isForDataType: Boolean = behaviorVersion.config.dataTypeName.isDefined

}

object BehaviorEditorData {

  def buildForEdit(
                    user: User,
                    behaviorId: String,
                    maybeJustSaved: Option[Boolean],
                    dataService: DataService,
                    ws: WSClient
                  ): Future[Option[BehaviorEditorData]] = {

    for {
      maybeBehaviorVersionData <- BehaviorVersionData.maybeFor(behaviorId, user, dataService)
      maybeTeam <- maybeBehaviorVersionData.map { data =>
        dataService.teams.find(data.teamId, user)
      }.getOrElse(Future.successful(None))
      maybeEditorData <- (for {
        data <- maybeBehaviorVersionData
        team <- maybeTeam
      } yield {
        buildFor(user, Some(data), team, maybeJustSaved, isForNewDataType = false, dataService, ws).map(Some(_))
      }).getOrElse(Future.successful(None))
    } yield maybeEditorData
  }

  def buildForNew(
                  user: User,
                  maybeTeamId: Option[String],
                  isForNewDataType: Boolean,
                  dataService: DataService,
                  ws: WSClient
                 ): Future[Option[BehaviorEditorData]] = {

    val teamId = maybeTeamId.getOrElse(user.teamId)
    dataService.teams.find(teamId, user).flatMap { maybeTeam =>
      maybeTeam.map { team =>
        buildFor(user, None, team, None, isForNewDataType, dataService, ws).map(Some(_))
      }.getOrElse(Future.successful(None))
    }
  }

  def buildFor(
                user: User,
                maybeBehaviorVersionData: Option[BehaviorVersionData],
                team: Team,
                maybeJustSaved: Option[Boolean],
                isForNewDataType: Boolean,
                dataService: DataService,
                ws: WSClient
              ): Future[BehaviorEditorData] = {
    for {
      teamAccess <- dataService.users.teamAccessFor(user, Some(team.id))
      teamEnvironmentVariables <- dataService.teamEnvironmentVariables.allFor(team)
      userEnvironmentVariables <- dataService.userEnvironmentVariables.allFor(user)
      oAuth2Applications <- dataService.oauth2Applications.allFor(team)
      oauth2Apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
      simpleTokenApis <- dataService.simpleTokenApis.allFor(teamAccess.maybeTargetTeam)
      linkedOAuth2Tokens <- dataService.linkedOAuth2Tokens.allForUser(user, ws)
      paramTypes <- teamAccess.maybeTargetTeam.map { team =>
        BehaviorParameterType.allFor(team, dataService)
      }.getOrElse(Future.successful(Seq()))
      paramTypeData <- Future.sequence(paramTypes.map(pt => BehaviorParameterTypeData.from(pt, dataService)))
    } yield {
      val maybeDataTypeName = if (isForNewDataType) { Some("") } else { None }
      val versionData = maybeBehaviorVersionData.getOrElse {
        BehaviorVersionData.buildFor(
          team.id,
          None,
          None,
          "",
          "",
          Seq(),
          Seq(),
          BehaviorConfig(None, None, None, None, None, maybeDataTypeName),
          None,
          None,
          None,
          dataService
        )
      }
      BehaviorEditorData(
        teamAccess,
        versionData,
        teamEnvironmentVariables.map(EnvironmentVariableData.withoutValueFor),
        paramTypeData,
        oAuth2Applications.map(OAuth2ApplicationData.from),
        oauth2Apis.map(OAuth2ApiData.from),
        simpleTokenApis.map(SimpleTokenApiData.from),
        linkedOAuth2Tokens.map(_.application.id),
        maybeJustSaved.exists(identity)
      )
    }
  }

}
