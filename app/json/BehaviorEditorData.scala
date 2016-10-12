package json

import models.accounts.user.{User, UserTeamAccess}

import scala.concurrent.ExecutionContext.Implicits.global
import models.behaviors.behaviorparameter.BehaviorParameterType
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

  def buildFor(
                user: User,
                maybeBehaviorId: Option[String],
                maybeTeamId: Option[String],
                maybeJustSaved: Option[Boolean],
                dataService: DataService
              ): Future[Option[BehaviorEditorData]] = {
    for {
      teamAccess <- dataService.users.teamAccessFor(user, maybeTeamId)
      maybeExistingVersionData <- maybeBehaviorId.map { id =>
        BehaviorVersionData.maybeFor(id, user, dataService)
      }.getOrElse(Future.successful(None))
      maybeEnvironmentVariables <- teamAccess.maybeTargetTeam.map { team =>
        dataService.environmentVariables.allFor(team).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeOAuth2Applications <- teamAccess.maybeTargetTeam.map { team =>
        dataService.oauth2Applications.allFor(team).map(Some(_))
      }.getOrElse(Future.successful(None))
      oauth2Apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
      paramTypes <- teamAccess.maybeTargetTeam.map { team =>
        BehaviorParameterType.allFor(team, dataService)
      }.getOrElse(Future.successful(Seq()))
      dataTypes <- teamAccess.maybeTargetTeam.map { team =>
        dataService.behaviorBackedDataTypes.allFor(team)
      }.getOrElse(Future.successful(Seq()))
    } yield {
      for {
        team <- teamAccess.maybeTargetTeam
        envVars <- maybeEnvironmentVariables
        oauth2Applications <- maybeOAuth2Applications
      } yield {
        val versionData = maybeExistingVersionData.getOrElse {
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
        val maybeDataTypeForBehavior = maybeBehaviorId.flatMap { id =>
          dataTypes.find(_.behavior.id == id)
        }
        BehaviorEditorData(
          teamAccess,
          versionData,
          envVars.map(EnvironmentVariableData.withoutValueFor),
          paramTypes.map(BehaviorParameterTypeData.from),
          oauth2Applications.map(OAuth2ApplicationData.from),
          oauth2Apis.map(OAuth2ApiData.from),
          maybeDataTypeForBehavior.map(BehaviorBackedDataTypeDataForBehavior.from),
          maybeJustSaved.exists(identity)
        )
      }
    }
  }

}
