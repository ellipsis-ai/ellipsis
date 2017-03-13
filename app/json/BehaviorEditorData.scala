package json

import java.time.OffsetDateTime

import models.accounts.user.{User, UserTeamAccess}
import models.behaviors.behaviorparameter.BehaviorParameterType
import models.team.Team
import play.api.libs.ws.WSClient
import services.DataService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class BehaviorEditorData(
                               teamAccess: UserTeamAccess,
                               group: BehaviorGroupData,
                               builtinParamTypes: Seq[BehaviorParameterTypeData],
                               maybeSelectedBehaviorId: Option[String],
                               environmentVariables: Seq[EnvironmentVariableData],
                               savedAnswers: Seq[InputSavedAnswerData],
                               oauth2Applications: Seq[OAuth2ApplicationData],
                               oauth2Apis: Seq[OAuth2ApiData],
                               simpleTokenApis: Seq[SimpleTokenApiData],
                               linkedOAuth2ApplicationIds: Seq[String]
                              )

object BehaviorEditorData {

  def buildForEdit(
                    user: User,
                    groupId: String,
                    maybeBehaviorId: Option[String],
                    dataService: DataService,
                    ws: WSClient
                  ): Future[Option[BehaviorEditorData]] = {

    for {
      maybeGroupData <- BehaviorGroupData.maybeFor(groupId, user, maybeGithubUrl = None, dataService)
      maybeTeam <- maybeGroupData.map { data =>
        dataService.teams.find(data.teamId, user)
      }.getOrElse(Future.successful(None))
      maybeEditorData <- (for {
        data <- maybeGroupData
        team <- maybeTeam
      } yield {
        buildFor(
          user,
          Some(data),
          maybeBehaviorId,
          team,
          dataService,
          ws
        ).map(Some(_))
      }).getOrElse(Future.successful(None))
    } yield maybeEditorData
  }

  def buildForNew(
                  user: User,
                  maybeTeamId: Option[String],
                  dataService: DataService,
                  ws: WSClient
                 ): Future[Option[BehaviorEditorData]] = {

    val teamId = maybeTeamId.getOrElse(user.teamId)
    for {
      maybeTeam <- dataService.teams.find(teamId, user)
      maybeData <- maybeTeam.map { team =>
        buildFor(
          user,
          maybeGroupData = None,
          maybeBehaviorId = None,
          team,
          dataService,
          ws
        ).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield maybeData
  }

  private def inputSavedAnswerDataFor(
                                     maybeBehaviorGroupData: Option[BehaviorGroupData],
                                     user: User,
                                     dataService: DataService
                                     ): Future[Seq[InputSavedAnswerData]] = {
    maybeBehaviorGroupData.map { data =>
      Future.sequence(data.behaviorVersions.map { behaviorVersionData =>
        Future.sequence(behaviorVersionData.params.flatMap { param =>
          param.inputVersionId.map { inputId =>
            InputSavedAnswerData.maybeFor(inputId, user, dataService)
          }
        }).map(_.flatten)
      }).map(_.flatten.distinct)
    }.getOrElse(Future.successful(Seq()))
  }

  def buildFor(
                user: User,
                maybeGroupData: Option[BehaviorGroupData],
                maybeBehaviorId: Option[String],
                team: Team,
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
      // TODO: use the group data or some such to avoid grabbing group from DB again
      maybeGroup <- maybeGroupData.flatMap { groupData =>
        groupData.id.map { groupId =>
          dataService.behaviorGroups.find(groupId)
        }
      }.getOrElse(Future.successful(None))
      maybeGroupVersion <- maybeGroup.map { group =>
        dataService.behaviorGroups.maybeCurrentVersionFor(group)
      }.getOrElse(Future.successful(None))
      inputSavedAnswerData <- inputSavedAnswerDataFor(maybeGroupData, user, dataService)
      // make sure the behavior exists and is accessible
      maybeRealBehaviorId <- maybeBehaviorId.map { behaviorId =>
        dataService.behaviors.find(behaviorId, user).map { maybeBehavior =>
          maybeBehavior.map(_.id)
        }
      }.getOrElse(Future.successful(None))
      builtinParamTypeData <- Future.sequence(BehaviorParameterType.allBuiltin.map(ea => BehaviorParameterTypeData.from(ea, dataService)))
    } yield {
      val data = maybeGroupData.getOrElse {
        BehaviorGroupData(
          None,
          team.id,
          name = None,
          description = None,
          icon = None,
          actionInputs = Seq(),
          dataTypeInputs = Seq(),
          Seq(BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, dataService)),
          githubUrl = None,
          exportId = None,
          Some(OffsetDateTime.now)
        )
      }
      BehaviorEditorData(
        teamAccess,
        data,
        builtinParamTypeData,
        maybeRealBehaviorId,
        teamEnvironmentVariables.map(EnvironmentVariableData.withoutValueFor),
        inputSavedAnswerData,
        oAuth2Applications.map(OAuth2ApplicationData.from),
        oauth2Apis.map(OAuth2ApiData.from),
        simpleTokenApis.map(SimpleTokenApiData.from),
        linkedOAuth2Tokens.map(_.application.id)
      )
    }
  }

}
