package json

import java.time.OffsetDateTime

import controllers.RemoteAssets
import models.accounts.user.{User, UserTeamAccess}
import models.behaviors.behaviorparameter.BehaviorParameterType
import models.team.Team
import play.api.libs.ws.WSClient
import services.{CacheService, DataService}

import scala.concurrent.{ExecutionContext, Future}

case class BehaviorEditorData(
                               teamAccess: UserTeamAccess,
                               group: BehaviorGroupData,
                               builtinParamTypes: Seq[BehaviorParameterTypeData],
                               maybeSelectedId: Option[String],
                               environmentVariables: Seq[EnvironmentVariableData],
                               savedAnswers: Seq[InputSavedAnswerData],
                               awsConfigs: Seq[AWSConfigData],
                               oauth2Applications: Seq[OAuth2ApplicationData],
                               oauth2Apis: Seq[OAuth2ApiData],
                               simpleTokenApis: Seq[SimpleTokenApiData],
                               linkedOAuth2ApplicationIds: Seq[String],
                               userId: String,
                               isAdmin: Boolean,
                               isLinkedToGithub: Boolean,
                               linkedGithubRepo: Option[LinkedGithubRepoData],
                               lastDeployTimestamp: Option[OffsetDateTime],
                               maybeSlackTeamId: Option[String]
                              )

object BehaviorEditorData {

  def buildForEdit(
                    user: User,
                    groupId: String,
                    maybeSelectedId: Option[String],
                    dataService: DataService,
                    cacheService: CacheService,
                    ws: WSClient,
                    assets: RemoteAssets
                  )(implicit ec: ExecutionContext): Future[Option[BehaviorEditorData]] = {

    for {
      maybeGroupData <- BehaviorGroupData.maybeFor(groupId, user, maybeGithubUrl = None, dataService, cacheService)
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
          maybeSelectedId,
          team,
          dataService,
          ws,
          assets
        ).map(Some(_))
      }).getOrElse(Future.successful(None))
    } yield maybeEditorData
  }

  def buildForNew(
                  user: User,
                  maybeTeamId: Option[String],
                  dataService: DataService,
                  ws: WSClient,
                  assets: RemoteAssets
                 )(implicit ec: ExecutionContext): Future[Option[BehaviorEditorData]] = {

    val teamId = maybeTeamId.getOrElse(user.teamId)
    for {
      maybeTeam <- dataService.teams.find(teamId, user)
      maybeData <- maybeTeam.map { team =>
        buildFor(
          user,
          maybeGroupData = None,
          maybeSelectedId = None,
          team,
          dataService,
          ws,
          assets
        ).map(Some(_))
      }.getOrElse(Future.successful(None))
    } yield maybeData
  }

  private def inputSavedAnswerDataFor(
                                     maybeBehaviorGroupData: Option[BehaviorGroupData],
                                     user: User,
                                     dataService: DataService
                                     )(implicit ec: ExecutionContext): Future[Seq[InputSavedAnswerData]] = {
    maybeBehaviorGroupData.map { data =>
      data.id.map { groupId =>
        for {
          maybeGroup <- dataService.behaviorGroups.findWithoutAccessCheck(groupId)
          maybeBehaviorGroupVersion <- maybeGroup.map { group =>
            dataService.behaviorGroups.maybeCurrentVersionFor(group)
          }.getOrElse(Future.successful(None))
          answersData <- maybeBehaviorGroupVersion.map { behaviorGroupVersion =>
            Future.sequence(data.inputs.map { inputData =>
              inputData.inputId.map { inputId =>
                InputSavedAnswerData.maybeFor(inputId, behaviorGroupVersion, user, dataService)
              }.getOrElse(Future.successful(None))
            }).map(_.flatten.distinct)
          }.getOrElse(Future.successful(Seq()))
        } yield answersData
      }.getOrElse(Future.successful(Seq()))
    }.getOrElse(Future.successful(Seq()))
  }

  def buildFor(
                user: User,
                maybeGroupData: Option[BehaviorGroupData],
                maybeSelectedId: Option[String],
                team: Team,
                dataService: DataService,
                ws: WSClient,
                assets: RemoteAssets
              )(implicit ec: ExecutionContext): Future[BehaviorEditorData] = {
    for {
      teamAccess <- dataService.users.teamAccessFor(user, Some(team.id))
      maybeSlackBotProfile <- dataService.slackBotProfiles.allFor(team).map(_.headOption)
      teamEnvironmentVariables <- dataService.teamEnvironmentVariables.allFor(team)
      awsConfigs <- dataService.awsConfigs.allFor(team)
      oAuth2Applications <- dataService.oauth2Applications.allUsableFor(team)
      oauth2Apis <- dataService.oauth2Apis.allFor(teamAccess.maybeTargetTeam)
      simpleTokenApis <- dataService.simpleTokenApis.allFor(teamAccess.maybeTargetTeam)
      linkedOAuth2Tokens <- dataService.linkedOAuth2Tokens.allForUser(user, ws)
      // TODO: use the group data or some such to avoid grabbing group from DB again
      maybeGroup <- maybeGroupData.flatMap { groupData =>
        groupData.id.map { groupId =>
          dataService.behaviorGroups.findWithoutAccessCheck(groupId)
        }
      }.getOrElse(Future.successful(None))
      maybeGroupVersion <- maybeGroup.map { group =>
        dataService.behaviorGroups.maybeCurrentVersionFor(group)
      }.getOrElse(Future.successful(None))
      inputSavedAnswerData <- inputSavedAnswerDataFor(maybeGroupData, user, dataService)
      // make sure the behavior exists and is accessible
      maybeVerifiedBehaviorId <- maybeSelectedId.map { selectedId =>
        dataService.behaviors.find(selectedId, user).map { maybeBehavior =>
          maybeBehavior.map(_.id)
        }
      }.getOrElse(Future.successful(None))
      maybeVerifiedLibraryId <- maybeSelectedId.flatMap { selectedId =>
        maybeGroupVersion.map { groupVersion =>
          dataService.libraries.findByLibraryId(selectedId, groupVersion, user).map { maybeLibraryVersion =>
            maybeLibraryVersion.map(_.libraryId)
          }
        }
      }.getOrElse(Future.successful(None))
      builtinParamTypeData <- Future.sequence(BehaviorParameterType.allBuiltin.map(ea => BehaviorParameterTypeData.from(ea, dataService)))
      userData <- dataService.users.userDataFor(user, team)
      isAdmin <- dataService.users.isAdmin(user)
      isLinkedToGithub <- dataService.linkedAccounts.maybeForGithubFor(user).map(_.nonEmpty)
      maybeLinkedGithubRepo <- maybeGroup.map { group =>
        dataService.linkedGithubRepos.maybeFor(group)
      }.getOrElse(Future.successful(None))
      maybeDeployment <- maybeGroupVersion.map { groupVersion =>
        dataService.behaviorGroupDeployments.findForBehaviorGroupVersion(groupVersion)
      }.getOrElse(Future.successful(None))
      maybeDeploymentData <- maybeDeployment.map { deployment =>
        BehaviorGroupDeploymentData.fromDeployment(deployment, dataService).map(Some(_))
      }.getOrElse(Future.successful(None))
      maybeLastDeployTimestamp <- maybeDeployment.map { deployment =>
        Future.successful(Some(deployment.createdAt))
      }.getOrElse {
        maybeGroup.map { group =>
          dataService.behaviorGroupDeployments.maybeMostRecentFor(group).map { maybeDeployment =>
            maybeDeployment.map(_.createdAt)
          }
        }.getOrElse(Future.successful(None))
      }
    } yield {
      val maybeVerifiedSelectedId = maybeVerifiedBehaviorId.orElse(maybeVerifiedLibraryId)
      val data = maybeGroupData.getOrElse {
        BehaviorGroupData(
          None,
          team.id,
          name = None,
          description = None,
          icon = None,
          actionInputs = Seq(),
          dataTypeInputs = Seq(),
          Seq(BehaviorVersionData.newUnsavedFor(team.id, isDataType = false, maybeName = None, dataService)),
          Seq(),
          Seq(),
          Seq(),
          Seq(),
          githubUrl = None,
          maybeGroupVersion.flatMap(_.maybeGitSHA),
          exportId = None,
          Some(OffsetDateTime.now),
          Some(userData),
          maybeDeploymentData
        )
      }
      BehaviorEditorData(
        teamAccess,
        data,
        builtinParamTypeData,
        maybeVerifiedSelectedId,
        teamEnvironmentVariables.map(EnvironmentVariableData.withoutValueFor),
        inputSavedAnswerData,
        awsConfigs.map(AWSConfigData.from),
        oAuth2Applications.map(OAuth2ApplicationData.from),
        oauth2Apis.map(ea => OAuth2ApiData.from(ea, assets)),
        simpleTokenApis.map(ea => SimpleTokenApiData.from(ea, assets)),
        linkedOAuth2Tokens.map(_.application.id),
        user.id,
        isAdmin,
        isLinkedToGithub,
        maybeLinkedGithubRepo.map(r => LinkedGithubRepoData(r.owner, r.repo, r.maybeCurrentBranch)),
        maybeLastDeployTimestamp,
        maybeSlackBotProfile.map(_.slackTeamId)
      )
    }
  }

}
