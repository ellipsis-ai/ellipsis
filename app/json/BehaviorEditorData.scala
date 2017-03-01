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
                               otherBehaviorsInGroup: Seq[BehaviorVersionData],
                               paramTypes: Seq[BehaviorParameterTypeData],
                               savedAnswers: Seq[InputSavedAnswerData],
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
      maybeGroup <- maybeBehaviorVersionData.flatMap { data =>
        data.groupId.map { gid =>
          dataService.behaviorGroups.find(gid)
        }
      }.getOrElse(Future.successful(None))
      maybeEditorData <- (for {
        data <- maybeBehaviorVersionData
        team <- maybeTeam
      } yield {
        buildFor(
          user,
          Some(data),
          data.groupId,
          maybeGroup.map(_.name),
          maybeGroup.flatMap(_.maybeDescription),
          team,
          maybeJustSaved,
          isForNewDataType = false,
          dataService,
          ws
        ).map(Some(_))
      }).getOrElse(Future.successful(None))
    } yield maybeEditorData
  }

  def buildForNew(
                  user: User,
                  maybeGroupId: Option[String],
                  maybeTeamId: Option[String],
                  isForNewDataType: Boolean,
                  dataService: DataService,
                  ws: WSClient
                 ): Future[Option[BehaviorEditorData]] = {

    val teamId = maybeTeamId.getOrElse(user.teamId)
    for {
      maybeTeam <- dataService.teams.find(teamId, user)
      maybeGroup <- maybeGroupId.map { gid =>
        dataService.behaviorGroups.find(gid)
      }.getOrElse(Future.successful(None))
      maybeData <- maybeTeam.map { team =>
        buildFor(
          user,
          None,
          maybeGroupId,
          maybeGroup.map(_.name),
          maybeGroup.flatMap(_.maybeDescription),
          team,
          None,
          isForNewDataType,
          dataService,
          ws
        ).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield maybeData
  }

  private def inputSavedAnswerDataFor(
                               maybeBehaviorVersionData: Option[BehaviorVersionData],
                               otherBehaviorsInGroupData: Seq[BehaviorVersionData],
                               user: User,
                               dataService: DataService
                             ): Future[Seq[InputSavedAnswerData]] = {
    val behaviorVersionDataForGroup = otherBehaviorsInGroupData ++ maybeBehaviorVersionData.map(Seq(_)).getOrElse(Seq())
    Future.sequence(behaviorVersionDataForGroup.map { behaviorVersionData =>
      Future.sequence(behaviorVersionData.params.flatMap { param =>
        param.inputId.map { inputId =>
          InputSavedAnswerData.maybeFor(inputId, user, dataService)
        }
      }).map(_.flatten)
    }).map(_.flatten.distinct)
  }

  def buildFor(
                user: User,
                maybeBehaviorVersionData: Option[BehaviorVersionData],
                maybeGroupId: Option[String],
                maybeGroupName: Option[String],
                maybeGroupDescription: Option[String],
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
      maybeGroup <- maybeGroupId.map { groupId =>
        dataService.behaviorGroups.find(groupId)
      }.getOrElse(Future.successful(None))
      otherBehaviorsInGroup <- maybeGroup.map { group =>
        dataService.behaviors.allForGroup(group).map { behaviors =>
          behaviors.filterNot(b => maybeBehaviorVersionData.exists(_.behaviorId.contains(b.id)))
        }
      }.getOrElse(Future.successful(Seq()))
      otherBehaviorsInGroupData <- Future.sequence(otherBehaviorsInGroup.map { ea =>
        BehaviorVersionData.maybeFor(ea.id, user, dataService)
      }).map(_.flatten)
      oAuth2Applications <- dataService.oauth2Applications.allFor(team)
      oauth2Apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
      simpleTokenApis <- dataService.simpleTokenApis.allFor(teamAccess.maybeTargetTeam)
      linkedOAuth2Tokens <- dataService.linkedOAuth2Tokens.allForUser(user, ws)
      paramTypes <- teamAccess.maybeTargetTeam.map { team =>
        BehaviorParameterType.allFor(maybeGroup, dataService)
      }.getOrElse(Future.successful(Seq()))
      paramTypeData <- Future.sequence(paramTypes.map(pt => BehaviorParameterTypeData.from(pt, dataService)))
      inputSavedAnswerData <- inputSavedAnswerDataFor(maybeBehaviorVersionData, otherBehaviorsInGroupData, user, dataService)
    } yield {
      val maybeDataTypeName = if (isForNewDataType) { Some("") } else { None }
      val versionData = maybeBehaviorVersionData.getOrElse {
        BehaviorVersionData.buildFor(
          team.id,
          maybeGroupId,
          maybeGroupName,
          maybeGroupDescription,
          None,
          None,
          "",
          "",
          Seq(),
          Seq(),
          BehaviorConfig(None, None, None, None, None, None, maybeDataTypeName),
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
        otherBehaviorsInGroupData,
        paramTypeData,
        inputSavedAnswerData,
        oAuth2Applications.map(OAuth2ApplicationData.from),
        oauth2Apis.map(OAuth2ApiData.from),
        simpleTokenApis.map(SimpleTokenApiData.from),
        linkedOAuth2Tokens.map(_.application.id),
        maybeJustSaved.exists(identity)
      )
    }
  }

}
