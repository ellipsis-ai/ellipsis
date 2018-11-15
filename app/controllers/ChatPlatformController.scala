package controllers

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import models.accounts.BotProfile
import models.behaviors.{ActionChoice, BotResult}
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.builtins.DisplayHelpBehavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageActionConstants.{BEHAVIOR_GROUP_HELP_RUN_BEHAVIOR_VERSION, SHOW_BEHAVIOR_GROUP_HELP}
import models.behaviors.events.{Event, MessageEvent}
import models.help.HelpGroupSearchValue
import play.api.Logger
import play.api.mvc.{AnyContent, Request, Result}
import services.caching.CacheService
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}


trait ChatPlatformController {

  trait ActionsTriggeredInfo {

    val channelId: String
    val userIdForContext: String
    val teamIdForContext: String
    val teamIdForUserForContext: String
    val maybeUserIdForDataTypeChoice: Option[String]
    val contextName: String
    def loginInfo: LoginInfo = LoginInfo(contextName, userIdForContext)

    def isIncorrectTeam(botProfile: BotProfileType): Future[Boolean]

    def formattedUserFor(permission: ActionPermission): String
    def instantBackgroundResponse(responseText: String, permission: ActionPermission): Future[Option[String]]
    def result(maybeResultText: Option[String], permission: ActionPermission): Result
    def processTriggerableAndActiveActionChoice(
                                                 actionChoice: ActionChoice,
                                                 maybeGroupVersion: Option[BehaviorGroupVersion],
                                                 botProfile: BotProfileType,
                                                 maybeInstantResponseTs: Option[String]
                                               ): Future[Unit]
    def sendCannotBeTriggeredFor(
                                  actionChoice: ActionChoice,
                                  maybeGroupVersion: Option[BehaviorGroupVersion]
                                ): Future[Unit]
    def maybeSelectedActionChoice: Option[ActionChoice]
    def updateActionsMessageFor(
                                 maybeResultText: Option[String],
                                 shouldRemoveActions: Boolean
                               ): Future[Unit]
    def sendEphemeralMessage(message: String): Future[Unit]
    def inputChoiceResultFor(value: String)(implicit request: Request[AnyContent]): Future[Unit]

    def isIncorrectUserTryingDataTypeChoice: Boolean
    def maybeDataTypeChoice: Option[String]
    def isForDataTypeChoiceForDoneConversation: Future[Boolean]

    def isIncorrectUserTryingYesNo: Boolean
    def maybeYesNoAnswer: Option[String]
    def isForYesNoForDoneConversation: Future[Boolean]

    def onEvent(event: Event): Future[Unit]

    def sendResultWithNewEvent(
                                description: String,
                                getEventualMaybeResult: MessageEvent => Future[Option[BotResult]],
                                botProfile: BotProfileType,
                                beQuiet: Boolean
                              ): Future[Option[String]]

    def maybeHelpIndexAt: Option[Int]

    def findButtonLabelForNameAndValue(name: String, value: String): Option[String]

    def maybeHelpForSkillIdWithMaybeSearch: Option[HelpGroupSearchValue]

    def maybeActionListForSkillId: Option[HelpGroupSearchValue]

    val maybeConfirmContinueConversationResponse: Option[ConfirmContinueConversationResponse]

    val maybeStopConversationResponse: Option[StopConversationResponse]

    def findOptionLabelForValue(value: String): Option[String]

    def maybeHelpRunBehaviorVersionId: Option[String]

  }

  type BotProfileType <: BotProfile
  type ActionsTriggeredInfoType <: ActionsTriggeredInfo

  val services: DefaultServices
  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService

  implicit val ec: ExecutionContext
  implicit val actorSystem: ActorSystem

  trait ActionPermission {

    val info: ActionsTriggeredInfoType
    val shouldRemoveActions: Boolean
    val maybeResultText: Option[String]
    val beQuiet: Boolean = false

    implicit val request: Request[AnyContent]

    def instantBackgroundResponse(responseText: String): Future[Option[String]] = {
      info.instantBackgroundResponse(responseText, this)
    }

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]): Unit

    def result: Result = info.result(maybeResultText, this)

  }

  trait ActionPermissionType[T <: ActionPermission] {

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Option[Future[T]]

    def maybeResultFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Option[Future[Result]] = {
      maybeFor(info, botProfile).map(_.map(_.result))
    }

  }

  case class ActionChoicePermission(
                                     actionChoice: ActionChoice,
                                     info: ActionsTriggeredInfoType,
                                     maybeGroupVersion: Option[BehaviorGroupVersion],
                                     isActive: Boolean,
                                     canBeTriggered: Boolean,
                                     botProfile: BotProfileType,
                                     implicit val request: Request[AnyContent]
                                   ) extends ActionPermission {

    override val beQuiet: Boolean = actionChoice.shouldBeQuiet

    override val maybeResultText: Option[String] = if (isActive) {
      Some(s"${info.formattedUserFor(this)} clicked ${actionChoice.label}")
    } else {
      Some("This skill has been updated, making these associated actions no longer valid")
    }

    override val shouldRemoveActions: Boolean = !actionChoice.allowMultipleSelections.exists(identity) && canBeTriggered

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]) = {
      if (canBeTriggered) {
        if (isActive) {
          for {
            maybeTs <- maybeInstantResponseTs
            result <- info.processTriggerableAndActiveActionChoice(actionChoice, maybeGroupVersion, botProfile, maybeTs)
          } yield result
        }
      } else {
        info.sendCannotBeTriggeredFor(actionChoice, maybeGroupVersion)
      }
    }
  }

  object ActionChoicePermission extends ActionPermissionType[ActionChoicePermission] {

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Option[Future[ActionChoicePermission]] = {
      info.maybeSelectedActionChoice.map { actionChoice =>
        buildFor(actionChoice, info, botProfile)
      }
    }

    def buildFor(actionChoice: ActionChoice, info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[ActionChoicePermission] = {
      for {
        maybeOriginatingBehaviorVersion <- dataService.behaviorVersions.findWithoutAccessCheck(actionChoice.originatingBehaviorVersionId)
        maybeGroupVersion <- Future.successful(maybeOriginatingBehaviorVersion.map(_.groupVersion))
        maybeActiveGroupVersion <- maybeGroupVersion.map { groupVersion =>
          dataService.behaviorGroupDeployments.maybeActiveBehaviorGroupVersionFor(groupVersion.group, info.contextName, info.channelId)
        }.getOrElse(Future.successful(None))
        isActive <- (for {
          groupVersion <- maybeGroupVersion
          activeVersion <- maybeActiveGroupVersion
        } yield {
          if (groupVersion == activeVersion) {
            Future.successful(true)
          } else {
            dataService.behaviorGroupVersions.haveActionsWithNameAndSameInterface(actionChoice.actionName, groupVersion, activeVersion)
          }
        }).getOrElse(Future.successful(false))
        canBeTriggered <- for {
          maybeUser <- dataService.users.ensureUserFor(info.loginInfo, botProfile.teamId).map(Some(_))
          canBeTriggered <- maybeUser.map { user =>
            actionChoice.canBeTriggeredBy(user, info.teamIdForUserForContext, botProfile.teamIdForContext, dataService)
          }.getOrElse(Future.successful(false))
        } yield canBeTriggered
      } yield ActionChoicePermission(
        actionChoice,
        info,
        maybeActiveGroupVersion,
        isActive,
        canBeTriggered,
        botProfile,
        request
      )
    }
  }

  trait InputChoicePermission extends ActionPermission {
    val choice: String
    val isConversationDone: Boolean
    val isIncorrectUser: Boolean
    val maybeResultText = Some(s"${info.formattedUserFor(this)} chose $choice")
    val shouldRemoveActions = true

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]) = {
      if (isConversationDone) {
        info.updateActionsMessageFor(Some(s"This conversation is no longer active"), shouldRemoveActions)
      } else if (isIncorrectUser) {
        info.maybeUserIdForDataTypeChoice.foreach { correctUserId =>
          val correctUser = s"<@${correctUserId}>"
          info.sendEphemeralMessage(s"Only $correctUser can answer this")
        }
      } else {
        info.inputChoiceResultFor(choice)
      }
    }
  }

  case class DataTypeChoicePermission(
                                       choice: String,
                                       info: ActionsTriggeredInfoType,
                                       isConversationDone: Boolean,
                                       implicit val request: Request[AnyContent]
                                     ) extends InputChoicePermission {
    val isIncorrectUser: Boolean = info.isIncorrectUserTryingDataTypeChoice
  }

  object DataTypeChoicePermission extends ActionPermissionType[DataTypeChoicePermission] {

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Option[Future[DataTypeChoicePermission]] = {
      info.maybeDataTypeChoice.map { choice =>
        buildFor(choice, info)
      }
    }

    def buildFor(choice: String, info: ActionsTriggeredInfoType)(implicit request: Request[AnyContent]): Future[DataTypeChoicePermission] = {
      for {
        isConversationDone <- info.isForDataTypeChoiceForDoneConversation
      } yield DataTypeChoicePermission(
        choice,
        info,
        isConversationDone,
        request
      )
    }

  }

  case class YesNoChoicePermission(
                                    choice: String,
                                    info: ActionsTriggeredInfoType,
                                    isConversationDone: Boolean,
                                    implicit val request: Request[AnyContent]
                                  ) extends InputChoicePermission {
    val isIncorrectUser: Boolean = info.isIncorrectUserTryingYesNo
  }

  object YesNoChoicePermission extends ActionPermissionType[YesNoChoicePermission] {

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Option[Future[YesNoChoicePermission]] = {
      info.maybeYesNoAnswer.map { value =>
        buildFor(value, info)
      }
    }

    def buildFor(value: String, info: ActionsTriggeredInfoType)(implicit request: Request[AnyContent]): Future[YesNoChoicePermission] = {
      for {
        isConversationDone <- info.isForYesNoForDoneConversation
      } yield YesNoChoicePermission(
        value,
        info,
        isConversationDone,
        request
      )
    }

  }

  trait HelpPermission extends ActionPermission {
    val shouldRemoveActions: Boolean = false
    val isIncorrectTeam: Boolean
    val botProfile: BotProfileType

    def runForCorrectTeam: Unit

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]): Unit = {
      if (isIncorrectTeam) {
        dataService.teams.find(botProfile.teamId).flatMap { maybeTeam =>
          val teamText = maybeTeam.map { team => s" ${team.name}"}.getOrElse("")
          val msg = s"Only members of the${teamText} team can make this choice"
          info.sendEphemeralMessage(msg)
        }
      } else {
        runForCorrectTeam
      }
    }

  }

  trait HelpPermissionType[T <: HelpPermission, V] extends ActionPermissionType[T] {

    def maybeValueFor(info: ActionsTriggeredInfoType): Option[V]
    def buildFor(
                  value: V,
                  info: ActionsTriggeredInfoType,
                  isIncorrectTeam: Boolean,
                  botProfile: BotProfileType
                )(implicit request:  Request[AnyContent]): T

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Option[Future[T]] = {
      maybeValueFor(info).map { v =>
        buildFor(v, info, botProfile)
      }
    }

    def buildFor(value: V, info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[T] = {
      info.isIncorrectTeam(botProfile).map { isIncorrectTeam =>
        buildFor(value, info, isIncorrectTeam, botProfile)
      }
    }
  }

  case class HelpIndexPermission(
                                  index: Int,
                                  info: ActionsTriggeredInfoType,
                                  isIncorrectTeam: Boolean,
                                  botProfile: BotProfileType,
                                  implicit val request: Request[AnyContent]
                                ) extends HelpPermission {

    val maybeResultText = Some(s"${info.formattedUserFor(this)} clicked More help.")

    def runForCorrectTeam: Unit = {
      info.sendResultWithNewEvent(
        "help index",
        (event) => DisplayHelpBehavior(
          None,
          None,
          Some(index),
          includeNameAndDescription = false,
          includeNonMatchingResults = false,
          isFirstTrigger = false,
          event,
          services
        ).result.map(Some(_)),
        botProfile,
        beQuiet = false
      )
    }

  }

  object HelpIndexPermission extends HelpPermissionType[HelpIndexPermission, Int] {

    def maybeValueFor(info: ActionsTriggeredInfoType): Option[Int] = info.maybeHelpIndexAt
    def buildFor(
                  value: Int,
                  info: ActionsTriggeredInfoType,
                  isIncorrectTeam: Boolean,
                  botProfile: BotProfileType
                )(implicit request:  Request[AnyContent]): HelpIndexPermission = {
      HelpIndexPermission(value, info, isIncorrectTeam, botProfile, request)
    }

  }

  case class HelpForSkillPermission(
                                     searchValue: HelpGroupSearchValue,
                                     info: ActionsTriggeredInfoType,
                                     isIncorrectTeam: Boolean,
                                     botProfile: BotProfileType,
                                     implicit val request: Request[AnyContent]
                                   ) extends HelpPermission {

    val maybeResultText = Some(info.findButtonLabelForNameAndValue(SHOW_BEHAVIOR_GROUP_HELP, searchValue.helpGroupId).map { text =>
      s"${info.formattedUserFor(this)} clicked $text."
    } getOrElse {
      s"${info.formattedUserFor(this)} clicked a button."
    })

    def runForCorrectTeam: Unit = {
      info.sendResultWithNewEvent(
        "skill help with maybe search",
        (event) => DisplayHelpBehavior(
          searchValue.maybeSearchText,
          Some(searchValue.helpGroupId),
          None,
          includeNameAndDescription = true,
          includeNonMatchingResults = false,
          isFirstTrigger = false,
          event,
          services
        ).result.map(Some(_)),
        botProfile,
        beQuiet = false
      )
    }

  }

  object HelpForSkillPermission extends HelpPermissionType[HelpForSkillPermission, HelpGroupSearchValue] {

    def maybeValueFor(info: ActionsTriggeredInfoType): Option[HelpGroupSearchValue] = info.maybeHelpForSkillIdWithMaybeSearch
    def buildFor(
                  value: HelpGroupSearchValue,
                  info: ActionsTriggeredInfoType,
                  isIncorrectTeam: Boolean,
                  botProfile: BotProfileType
                )(implicit request:  Request[AnyContent]): HelpForSkillPermission = {
      HelpForSkillPermission(value, info, isIncorrectTeam, botProfile, request)
    }

  }

  case class HelpListAllActionsPermission(
                                           searchValue: HelpGroupSearchValue,
                                           info: ActionsTriggeredInfoType,
                                           isIncorrectTeam: Boolean,
                                           botProfile: BotProfileType,
                                           implicit val request: Request[AnyContent]
                                         ) extends HelpPermission {

    val maybeResultText = Some(s"${info.formattedUserFor(this)} clicked List all actions")

    def runForCorrectTeam: Unit = {
      info.sendResultWithNewEvent(
        "for skill action list",
        event => DisplayHelpBehavior(
          searchValue.maybeSearchText,
          Some(searchValue.helpGroupId),
          None,
          includeNameAndDescription = false,
          includeNonMatchingResults = true,
          isFirstTrigger = false,
          event,
          services
        ).result.map(Some(_)),
        botProfile,
        beQuiet = false
      )
    }

  }

  object HelpListAllActionsPermission extends HelpPermissionType[HelpListAllActionsPermission, HelpGroupSearchValue] {

    def maybeValueFor(info: ActionsTriggeredInfoType): Option[HelpGroupSearchValue] = info.maybeActionListForSkillId
    def buildFor(
                  value: HelpGroupSearchValue,
                  info: ActionsTriggeredInfoType,
                  isIncorrectTeam: Boolean,
                  botProfile: BotProfileType
                )(implicit request: Request[AnyContent]): HelpListAllActionsPermission = {
      HelpListAllActionsPermission(value, info, isIncorrectTeam, botProfile, request)
    }

  }

  trait ConversationActionPermission extends ActionPermission {
    val correctUserId: String
    lazy val isCorrectUser: Boolean = correctUserId == info.userIdForContext
    lazy val correctUser: String = s"<@$correctUserId>"
    lazy val shouldRemoveActions: Boolean = isCorrectUser

    def runForCorrectUser(): Unit

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]): Unit = {
      if (isCorrectUser) {
        runForCorrectUser()
      } else {
        info.sendEphemeralMessage(s"Only $correctUser can do this")
      }
    }
  }

  case class ConfirmContinueConversationResponse(shouldContinue: Boolean, conversationId: String, userId: String)

  case class StopConversationResponse(conversationId: String, userId: String)

  case class ConfirmContinueConversationPermission(
                                                    response: ConfirmContinueConversationResponse,
                                                    info: ActionsTriggeredInfoType,
                                                    implicit val request: Request[AnyContent]
                                                  ) extends ConversationActionPermission {

    val maybeResultText: Option[String] = {
      val r = if (response.shouldContinue) { "Yes" } else { "No" }
      Some(s"${info.formattedUserFor(this)} clicked '$r'")
    }

    val correctUserId: String = response.userId

    def continue(conversation: Conversation): Future[Unit] = {
      dataService.conversations.touch(conversation).flatMap { _ =>
        cacheService.getEvent(conversation.pendingEventKey).map { event =>
          info.onEvent(event)
        }.getOrElse(Future.successful({}))
      }
    }

    def dontContinue(conversation: Conversation): Future[Unit] = {
      dataService.conversations.background(conversation, "OK, on to the next thing.", includeUsername = false).flatMap { _ =>
        cacheService.getEvent(conversation.pendingEventKey).map { event =>
          services.eventHandler.handle(event, None).flatMap { results =>
            Future.sequence(
              results.map(result => services.botResultService.sendIn(result, None).map { _ =>
                Logger.info(event.logTextFor(result, None))
              })
            )
          }.map(_ => {})
        }.getOrElse(Future.successful({}))
      }
    }

    def runForCorrectUser(): Unit = {
      dataService.conversations.find(response.conversationId).flatMap { maybeConversation =>
        maybeConversation.map { convo =>
          if (response.shouldContinue) {
            continue(convo)
          } else {
            dontContinue(convo)
          }
        }.getOrElse(Future.successful({}))
      }
    }
  }

  object ConfirmContinueConversationPermission extends ActionPermissionType[ConfirmContinueConversationPermission] {

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Option[Future[ConfirmContinueConversationPermission]] = {
      info.maybeConfirmContinueConversationResponse.map { response =>
        buildFor(response, info)
      }
    }

    def buildFor(response: ConfirmContinueConversationResponse, info: ActionsTriggeredInfoType)(implicit request: Request[AnyContent]): Future[ConfirmContinueConversationPermission] = {
      Future.successful(ConfirmContinueConversationPermission(
        response,
        info,
        request
      ))
    }

  }

  case class StopConversationPermission(
                                         response: StopConversationResponse,
                                         info: ActionsTriggeredInfoType,
                                         implicit val request: Request[AnyContent]
                                       ) extends ConversationActionPermission {

    val correctUserId: String = response.userId

    val maybeResultText = Some(s"${info.formattedUserFor(this)} clicked 'Stop asking'")

    def runForCorrectUser(): Unit = {
      dataService.conversations.find(response.conversationId).flatMap { maybeConversation =>
        maybeConversation.map { convo =>
          dataService.conversations.cancel(convo)
        }.getOrElse(Future.successful({}))
      }
    }

  }

  object StopConversationPermission extends ActionPermissionType[StopConversationPermission] {

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Option[Future[StopConversationPermission]] = {
      info.maybeStopConversationResponse.map { response =>
        buildFor(response, info)
      }
    }

    def buildFor(response: StopConversationResponse, info: ActionsTriggeredInfoType)(implicit request: Request[AnyContent]): Future[StopConversationPermission] = {
      Future.successful(StopConversationPermission(
        response,
        info,
        request
      ))
    }

  }

  case class HelpRunBehaviorVersionPermission(
                                               behaviorVersionId: String,
                                               info: ActionsTriggeredInfoType,
                                               isActive: Boolean,
                                               canBeTriggered: Boolean,
                                               botProfile: BotProfileType,
                                               implicit val request: Request[AnyContent]
                                             ) extends ActionPermission {

    val shouldRemoveActions: Boolean = false
    val maybeOptionLabel: Option[String] = info.findOptionLabelForValue(behaviorVersionId).map(_.mkString("“", "", "”"))
    val maybeResultText = Some({
      val actionText = maybeOptionLabel.getOrElse("an action")
      if (!isActive) {
        s"${info.formattedUserFor(this)} tried to run an obsolete version of $actionText – run help again to get the latest actions"
      } else if (!canBeTriggered) {
        s"${info.formattedUserFor(this)} tried to run $actionText"
      } else {
        info.findButtonLabelForNameAndValue(BEHAVIOR_GROUP_HELP_RUN_BEHAVIOR_VERSION, behaviorVersionId).map { text =>
          s"${info.formattedUserFor(this)} clicked $text"
        }.getOrElse {
          s"${info.formattedUserFor(this)} ran $actionText"
        }
      }
    })

    private def runBehaviorVersion(): Unit = {
      info.sendResultWithNewEvent(
        s"run behavior version $behaviorVersionId",
        event => for {
          maybeBehaviorVersion <- dataService.behaviorVersions.findWithoutAccessCheck(behaviorVersionId)
          maybeResponse <- maybeBehaviorVersion.map { behaviorVersion =>
            dataService.behaviorResponses.buildFor(
              event,
              behaviorVersion,
              Map(),
              None,
              None,
              None,
              userExpectsResponse = true
            ).map(Some(_))
          }.getOrElse(Future.successful(None))
          maybeResult <- maybeResponse.map { response =>
            response.result.map(Some(_))
          }.getOrElse(Future.successful(None))
        } yield maybeResult,
        botProfile,
        beQuiet = false
      )
    }

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]): Unit = {
      if (canBeTriggered) {
        if (isActive) {
          runBehaviorVersion()
        }
      } else {
        dataService.behaviorVersions.findWithoutAccessCheck(behaviorVersionId).flatMap { maybeBehaviorVersion =>
          val teamText = maybeBehaviorVersion.map { bv => s" ${bv.team.name}" }.getOrElse("")
          info.sendEphemeralMessage(s"Only members of the$teamText team can run this")
        }
      }

    }
  }

  object HelpRunBehaviorVersionPermission extends ActionPermissionType[HelpRunBehaviorVersionPermission] {

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Option[Future[HelpRunBehaviorVersionPermission]] = {
      info.maybeHelpRunBehaviorVersionId.map { behaviorVersionId =>
        buildFor(behaviorVersionId, info, botProfile)
      }
    }

    def buildFor(behaviorVersionId: String, info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[HelpRunBehaviorVersionPermission] = {
      for {
        maybeBehaviorVersion <- dataService.behaviorVersions.findWithoutAccessCheck(behaviorVersionId)
        isActive <- maybeBehaviorVersion.map { behaviorVersion =>
          dataService.behaviorGroupVersions.isActive(behaviorVersion.groupVersion, info.contextName, info.channelId)
        }.getOrElse(Future.successful(false))
        maybeUser <- dataService.users.ensureUserFor(info.loginInfo, botProfile.teamId).map(Some(_))
        canBeTriggered <- (for {
          behaviorVersion <- maybeBehaviorVersion
          user <- maybeUser
        } yield behaviorVersion.groupVersion.canBeTriggeredBy(user, dataService)).getOrElse(Future.successful(false))
      } yield {
        HelpRunBehaviorVersionPermission(
          behaviorVersionId,
          info,
          isActive = isActive,
          canBeTriggered = canBeTriggered,
          botProfile,
          request
        )
      }
    }

  }

  def maybePermissionResultFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Option[Future[Result]] = {
    DataTypeChoicePermission.maybeResultFor(info, botProfile).orElse {
      YesNoChoicePermission.maybeResultFor(info, botProfile).orElse {
        ActionChoicePermission.maybeResultFor(info, botProfile).orElse {
          HelpIndexPermission.maybeResultFor(info, botProfile).orElse {
            HelpForSkillPermission.maybeResultFor(info, botProfile).orElse {
              HelpListAllActionsPermission.maybeResultFor(info, botProfile).orElse {
                ConfirmContinueConversationPermission.maybeResultFor(info, botProfile).orElse {
                  StopConversationPermission.maybeResultFor(info, botProfile).orElse {
                    HelpRunBehaviorVersionPermission.maybeResultFor(info, botProfile).orElse {
                      None
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
