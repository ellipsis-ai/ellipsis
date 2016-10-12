package json

import models.accounts.user.{User, UserTeamAccess}

import scala.concurrent.ExecutionContext.Implicits.global
import models.behaviors.behaviorparameter.BehaviorParameterType
import models.team.Team
import services.DataService

import scala.concurrent.Future

case class BehaviorEditorData(
                               teamAccess: UserTeamAccess,
                               behaviorVersion: BehaviorVersionData,
                               environmentVariables: Seq[EnvironmentVariableData],
                               paramTypes: Seq[BehaviorParameterTypeData],
                               oauth2Applications: Seq[OAuth2ApplicationData],
                               oauth2Apis: Seq[OAuth2ApiData],
                               dataType: Option[BehaviorBackedDataTypeDataForBehavior],
                               justSaved: Boolean
                              )

object BehaviorEditorData {

  def buildForEdit(
                    user: User,
                    behaviorId: String,
                    maybeJustSaved: Option[Boolean],
                    dataService: DataService
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
        buildFor(user, Some(data), team, maybeJustSaved, dataService).map(Some(_))
      }).getOrElse(Future.successful(None))
    } yield maybeEditorData
  }

  def buildForNew(
                  user: User,
                  maybeTeamId: Option[String],
                  dataService: DataService
                 ): Future[Option[BehaviorEditorData]] = {

    val teamId = maybeTeamId.getOrElse(user.teamId)
    dataService.teams.find(teamId, user).flatMap { maybeTeam =>
      maybeTeam.map { team =>
        buildFor(user, None, team, None, dataService).map(Some(_))
      }.getOrElse(Future.successful(None))
    }
  }

  def buildFor(
                user: User,
                maybeBehaviorVersionData: Option[BehaviorVersionData],
                team: Team,
                maybeJustSaved: Option[Boolean],
                dataService: DataService
              ): Future[BehaviorEditorData] = {
    for {
      teamAccess <- dataService.users.teamAccessFor(user, Some(team.id))
      environmentVariables <- dataService.environmentVariables.allFor(team)
      oAuth2Applications <- dataService.oauth2Applications.allFor(team)
      oauth2Apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
      paramTypes <- teamAccess.maybeTargetTeam.map { team =>
        BehaviorParameterType.allFor(team, dataService)
      }.getOrElse(Future.successful(Seq()))
      dataTypes <- teamAccess.maybeTargetTeam.map { team =>
        dataService.behaviorBackedDataTypes.allFor(team)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      val versionData = maybeBehaviorVersionData.getOrElse {
        BehaviorVersionData.buildFor(
          team.id,
          None,
          "",
          "",
          Seq(),
          Seq(),
          BehaviorConfig(None, None, None),
          None,
          None,
          None,
          dataService
        )
      }
      val maybeDataTypeForBehavior = maybeBehaviorVersionData.flatMap { data =>
        dataTypes.find(_.behavior.id == data.behaviorId)
      }
      BehaviorEditorData(
        teamAccess,
        versionData,
        environmentVariables.map(EnvironmentVariableData.withoutValueFor),
        paramTypes.map(BehaviorParameterTypeData.from),
        oAuth2Applications.map(OAuth2ApplicationData.from),
        oauth2Apis.map(OAuth2ApiData.from),
        maybeDataTypeForBehavior.map(BehaviorBackedDataTypeDataForBehavior.from),
        maybeJustSaved.exists(identity)
      )
    }
  }

}
