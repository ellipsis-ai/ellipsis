package controllers

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import json.DialogState
import models.accounts.BotProfile
import models.behaviors.{ActionChoice, BotResult}
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.builtins.DisplayHelpBehavior
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageActionConstants.{BEHAVIOR_GROUP_HELP_RUN_BEHAVIOR_VERSION, SHOW_BEHAVIOR_GROUP_HELP}
import models.behaviors.events.{Event, EventType, MessageEvent}
import models.help.HelpGroupSearchValue
import play.api.Logger
import play.api.mvc.{AnyContent, Request, Result}
import services.caching.CacheService
import services.{DataService, DefaultServices}

import scala.concurrent.{ExecutionContext, Future}


trait ChatPlatformController {

  trait InteractionInfo {
    def loginInfo: LoginInfo
    def otherLoginInfos: Seq[LoginInfo]
    def loginInfos: Seq[LoginInfo] = Seq(loginInfo) ++ otherLoginInfos
    val contextName: String
    val channelId: String
    val teamIdForContext: String
    val teamIdForUserForContext: String

    def instantBackgroundResponse(
                                   responseText: String,
                                   permission: InteractionPermission,
                                   originalMessageId: String,
                                   maybeOriginalMessageThreadId: Option[String]
                                 ): Future[Option[String]]
  }

  trait ActionsTriggeredInfo extends InteractionInfo {

    val maybeUserIdForDataTypeChoice: Option[String]
    val maybeDialogTriggerId: Option[String]

    def isIncorrectTeam(botProfile: BotProfileType): Future[Boolean]

    def formattedUserFor(permission: ActionPermission): String
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
    def maybeSelectedActionChoice: Future[Option[ActionChoice]]
    def updateActionsMessageFor(
                                 maybeResultText: Option[String],
                                 shouldRemoveActions: Boolean,
                                 botProfile: BotProfileType
                               ): Future[Unit]
    def sendEphemeralMessage(message: String): Future[Unit]
    def inputChoiceResultFor(value: String, maybeResultText: Option[String])(implicit request: Request[AnyContent]): Future[Unit]

    def isIncorrectUserTryingDataTypeChoice: Boolean
    def maybeDataTypeChoice: Future[Option[String]]
    def isForDataTypeChoiceForDoneConversation: Future[Boolean]

    def isIncorrectUserTryingYesNo: Boolean
    def maybeYesNoAnswer: Future[Option[String]]
    def isForYesNoForDoneConversation: Future[Boolean]

    def maybeTextInputAnswer: Option[String]
    def isForTextInputForDoneConversation: Future[Boolean]

    def onEvent(event: Event): Future[Unit]

    def sendResultWithNewEvent(
                                description: String,
                                maybeOriginalEventType: Option[EventType],
                                getEventualMaybeResult: MessageEvent => Future[Option[BotResult]],
                                botProfile: BotProfileType,
                                beQuiet: Boolean
                              ): Future[Option[String]]

    def maybeHelpIndexAt: Future[Option[Int]]

    def findButtonLabelForNameAndValue(name: String, value: String): Option[String]

    def maybeHelpForSkillIdWithMaybeSearch: Future[Option[HelpGroupSearchValue]]

    def maybeActionListForSkillId: Future[Option[HelpGroupSearchValue]]

    def maybeConfirmContinueConversationResponse: Future[Option[ConfirmContinueConversationResponse]]

    val maybeStopConversationResponse: Option[StopConversationResponse]

    def findOptionLabelForValue(value: String): Option[String]

    def maybeHelpRunBehaviorVersionId: Future[Option[String]]

  }

  type BotProfileType <: BotProfile
  type ActionsTriggeredInfoType <: ActionsTriggeredInfo

  trait DialogSubmissionInfo extends InteractionInfo {
    val behaviorVersionId: String
    val parameters: Map[String, String]
    val maybeDialogState: Option[DialogState]

    def dialogSubmissionResult(permission: DialogSubmissionPermission): Future[Unit]

    def result(maybeResultText: Option[String], permission: DialogSubmissionPermission): Future[Result]
  }

  type DialogSubmissionInfoType <: DialogSubmissionInfo

  val services: DefaultServices
  val dataService: DataService = services.dataService
  val cacheService: CacheService = services.cacheService

  implicit val ec: ExecutionContext
  implicit val actorSystem: ActorSystem

  trait InteractionPermission {
    implicit val request: Request[AnyContent]
    val maybeResultText: Option[String]
    val beQuiet: Boolean = false
  }

  trait ActionPermission extends InteractionPermission {

    val info: ActionsTriggeredInfoType
    val shouldRemoveActions: Boolean

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]): Unit

    def result: Result = info.result(maybeResultText, this)

  }

  trait ActionPermissionType[T <: ActionPermission] {

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[T]]

    def maybeResultFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[Result]] = {
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

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[ActionChoicePermission]] = {
      info.maybeSelectedActionChoice.flatMap { maybeActionChoice =>
        maybeActionChoice.map { actionChoice =>
          buildFor(actionChoice, info, botProfile).map(Some(_))
        }.getOrElse(Future.successful(None))
      }
    }

    def buildFor(actionChoice: ActionChoice, info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[ActionChoicePermission] = {
      for {
        user <- dataService.users.ensureUserFor(info.loginInfo, info.otherLoginInfos, botProfile.teamId)
        maybeOriginatingBehaviorVersion <- dataService.behaviorVersions.findWithoutAccessCheck(actionChoice.originatingBehaviorVersionId)
        maybeOtherBehaviorGroup <- actionChoice.skillId.map { otherSkillId =>
          dataService.behaviorGroups.find(otherSkillId, user)
        }.getOrElse(Future.successful(None))
        maybeOtherBehaviorGroupVersion <- maybeOtherBehaviorGroup.map { otherBehaviorGroup =>
          dataService.behaviorGroups.maybeCurrentVersionFor(otherBehaviorGroup)
        }.getOrElse(Future.successful(None))
        maybeGroupVersion <- Future.successful(maybeOtherBehaviorGroupVersion.orElse(maybeOriginatingBehaviorVersion.map(_.groupVersion)))
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
          maybeUser <- dataService.users.ensureUserFor(info.loginInfo, info.otherLoginInfos, botProfile.teamId).map(Some(_))
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
    def chooseVerb: String = "chose"
    val maybeResultText = Some(s"${info.formattedUserFor(this)} $chooseVerb $choice")
    val shouldRemoveActions = true
    val botProfile: BotProfileType

    def runInBackground(maybeInstantResponseTs: Future[Option[String]]) = {
      if (isConversationDone) {
        info.updateActionsMessageFor(Some(s"This conversation is no longer active"), shouldRemoveActions, botProfile)
      } else if (isIncorrectUser) {
        info.maybeUserIdForDataTypeChoice.foreach { correctUserId =>
          val correctUser = s"<@${correctUserId}>"
          info.sendEphemeralMessage(s"Only $correctUser can answer this")
        }
      } else {
        info.inputChoiceResultFor(choice, maybeResultText)
      }
    }
  }

  case class DataTypeChoicePermission(
                                       choice: String,
                                       info: ActionsTriggeredInfoType,
                                       isConversationDone: Boolean,
                                       botProfile: BotProfileType,
                                       implicit val request: Request[AnyContent]
                                     ) extends InputChoicePermission {
    val isIncorrectUser: Boolean = info.isIncorrectUserTryingDataTypeChoice
  }

  object DataTypeChoicePermission extends ActionPermissionType[DataTypeChoicePermission] {

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[DataTypeChoicePermission]] = {
      info.maybeDataTypeChoice.flatMap { maybeChoice =>
        maybeChoice.map { choice =>
          buildFor(choice, info, botProfile).map(Some(_))
        }.getOrElse(Future.successful(None))
      }
    }

    def buildFor(choice: String, info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[DataTypeChoicePermission] = {
      for {
        isConversationDone <- info.isForDataTypeChoiceForDoneConversation
      } yield DataTypeChoicePermission(
        choice,
        info,
        isConversationDone,
        botProfile,
        request
      )
    }

  }

  case class YesNoChoicePermission(
                                    choice: String,
                                    info: ActionsTriggeredInfoType,
                                    isConversationDone: Boolean,
                                    botProfile: BotProfileType,
                                    implicit val request: Request[AnyContent]
                                  ) extends InputChoicePermission {
    val isIncorrectUser: Boolean = info.isIncorrectUserTryingYesNo
  }

  object YesNoChoicePermission extends ActionPermissionType[YesNoChoicePermission] {

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[YesNoChoicePermission]] = {
      info.maybeYesNoAnswer.flatMap { maybeValue =>
        maybeValue.map { value =>
          buildFor(value, info, botProfile).map(Some(_))
        }.getOrElse(Future.successful(None))
      }
    }

    def buildFor(value: String, info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[YesNoChoicePermission] = {
      for {
        isConversationDone <- info.isForYesNoForDoneConversation
      } yield YesNoChoicePermission(
        value,
        info,
        isConversationDone,
        botProfile,
        request
      )
    }

  }

  case class TextInputPermission(
                                    choice: String,
                                    info: ActionsTriggeredInfoType,
                                    isConversationDone: Boolean,
                                    botProfile: BotProfileType,
                                    implicit val request: Request[AnyContent]
                                  ) extends InputChoicePermission {
    val isIncorrectUser: Boolean = info.isIncorrectUserTryingYesNo
    override def chooseVerb: String = "entered"
  }

  object TextInputPermission extends ActionPermissionType[TextInputPermission] {

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[TextInputPermission]] = {
      info.maybeTextInputAnswer.map { value =>
        buildFor(value, info, botProfile).map(Some(_))
      }.getOrElse(Future.successful(None))
    }

    def buildFor(value: String, info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[TextInputPermission] = {
      for {
        isConversationDone <- info.isForTextInputForDoneConversation
      } yield TextInputPermission(
        value,
        info,
        isConversationDone,
        botProfile,
        request
      )
    }

  }

  case class DialogSubmissionPermission(
                                         info: DialogSubmissionInfoType,
                                         maybeBehaviorVersion: Option[BehaviorVersion],
                                         maybeGroupVersion: Option[BehaviorGroupVersion],
                                         isActive: Boolean,
                                         botProfile: BotProfileType,
                                         implicit val request: Request[AnyContent]
                                       ) extends InteractionPermission {
    val maybeResultText: Option[String] = if (isActive) {
      None
    } else {
      Some("The original action is no longer available. Please try opening the dialog again.")
    }

    def result: Future[Result] = {
      info.result(maybeResultText, this)
    }
  }

  object DialogSubmissionPermission {
    def maybeFor(info: DialogSubmissionInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[DialogSubmissionPermission]] = {
      buildFor(info, botProfile).map(Some(_))
    }

    def maybeResultFor(info: DialogSubmissionInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[Result]] = {
      for {
        maybePermission <- maybeFor(info, botProfile)
        maybeResult <- maybePermission.map(_.result.map(Some(_))).getOrElse(Future.successful(None))
      } yield maybeResult
    }

    def buildFor(info: DialogSubmissionInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[DialogSubmissionPermission] = {
      for {
        user <- dataService.users.ensureUserFor(info.loginInfo, info.otherLoginInfos, botProfile.teamId)
        maybeBehaviorVersion <- dataService.behaviorVersions.find(info.behaviorVersionId, user)
        maybeActiveGroupVersion <- maybeBehaviorVersion.map { behaviorVersion =>
          dataService.behaviorGroupDeployments.maybeActiveBehaviorGroupVersionFor(behaviorVersion.group, info.contextName, info.channelId)
        }.getOrElse(Future.successful(None))
        isActive <- (for {
          behaviorVersion <- maybeBehaviorVersion
          behaviorName <- behaviorVersion.maybeName
          activeVersion <- maybeActiveGroupVersion
        } yield {
          if (behaviorVersion.groupVersion == activeVersion) {
            Future.successful(true)
          } else {
            dataService.behaviorGroupVersions.haveActionsWithNameAndSameInterface(behaviorName, behaviorVersion.groupVersion, activeVersion)
          }
        }).getOrElse(Future.successful(false))
      } yield {
        DialogSubmissionPermission(info, maybeBehaviorVersion, maybeActiveGroupVersion, isActive, botProfile, request)
      }
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

    def maybeValueFor(info: ActionsTriggeredInfoType): Future[Option[V]]
    def buildFor(
                  value: V,
                  info: ActionsTriggeredInfoType,
                  isIncorrectTeam: Boolean,
                  botProfile: BotProfileType
                )(implicit request:  Request[AnyContent]): T

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[T]] = {
      maybeValueFor(info).flatMap { maybeValue =>
        maybeValue.map { v =>
          buildFor(v, info, botProfile).map(Some(_))
        }.getOrElse(Future.successful(None))
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
        None,
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

    def maybeValueFor(info: ActionsTriggeredInfoType): Future[Option[Int]] = info.maybeHelpIndexAt
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
        None,
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

    def maybeValueFor(info: ActionsTriggeredInfoType): Future[Option[HelpGroupSearchValue]] = info.maybeHelpForSkillIdWithMaybeSearch
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
        None,
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

    def maybeValueFor(info: ActionsTriggeredInfoType): Future[Option[HelpGroupSearchValue]] = info.maybeActionListForSkillId
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
    lazy val isCorrectUser: Boolean = info.loginInfos.map(_.providerKey).contains(correctUserId)
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
      for {
        _ <- dataService.conversations.touch(conversation)
        maybeEvent <- cacheService.getEvent(conversation.pendingEventKey)
        _ <- maybeEvent.map { event =>
          info.onEvent(event)
        }.getOrElse(Future.successful({}))
      } yield {}
    }

    def dontContinue(conversation: Conversation): Future[Unit] = {
      for {
        _ <- dataService.conversations.background(conversation, "OK, on to the next thing.", includeUsername = false)
        maybeEvent <- cacheService.getEvent(conversation.pendingEventKey)
        results <- maybeEvent.map { event =>
          services.eventHandler.handle(event, None)
        }.getOrElse(Future.successful(Seq()))
        _ <- Future.sequence(
          results.map(result => services.botResultService.sendIn(result, None).map { _ =>
            maybeEvent.foreach { event =>
              Logger.info(event.logTextFor(result, None))
            }
          })
        )
      } yield {}
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

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[ConfirmContinueConversationPermission]] = {
      info.maybeConfirmContinueConversationResponse.flatMap { maybeResponse =>
        maybeResponse.map { response =>
          buildFor(response, info).map(Some(_))
        }.getOrElse(Future.successful(None))
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

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[StopConversationPermission]] = {
      info.maybeStopConversationResponse.map { response =>
        buildFor(response, info).map(Some(_))
      }.getOrElse(Future.successful(None))
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
        None,
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

    def maybeFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[HelpRunBehaviorVersionPermission]] = {
      info.maybeHelpRunBehaviorVersionId.flatMap { maybeBehaviorVersionId =>
        maybeBehaviorVersionId.map { behaviorVersionId =>
          buildFor(behaviorVersionId, info, botProfile).map(Some(_))
        }.getOrElse(Future.successful(None))
      }
    }

    private def canBeTriggered(maybeBehaviorVersion: Option[BehaviorVersion], info: ActionsTriggeredInfoType, botProfile: BotProfileType): Future[Boolean] = {
      if (botProfile.supportsSharedChannels) {
        for {
          maybeUser <- dataService.users.ensureUserFor(info.loginInfo, info.otherLoginInfos, botProfile.teamId).map(Some(_))
          canBeTriggered <- (for {
            behaviorVersion <- maybeBehaviorVersion
            user <- maybeUser
          } yield behaviorVersion.groupVersion.canBeTriggeredBy(user, dataService)).getOrElse(Future.successful(false))
        } yield canBeTriggered
      } else {
        Future.successful(true)
      }
    }

    def buildFor(behaviorVersionId: String, info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[HelpRunBehaviorVersionPermission] = {
      for {
        maybeBehaviorVersion <- dataService.behaviorVersions.findWithoutAccessCheck(behaviorVersionId)
        isActive <- maybeBehaviorVersion.map { behaviorVersion =>
          dataService.behaviorGroupVersions.isActive(behaviorVersion.groupVersion, info.contextName, info.channelId)
        }.getOrElse(Future.successful(false))
        canBeTriggered <- canBeTriggered(maybeBehaviorVersion, info, botProfile)
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

  def maybeDialogPermissionResultFor(info: DialogSubmissionInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[Result]] = {
    DialogSubmissionPermission.maybeResultFor(info, botProfile)
  }

  def maybeActionPermissionResultFor(info: ActionsTriggeredInfoType, botProfile: BotProfileType)(implicit request: Request[AnyContent]): Future[Option[Result]] = {
    DataTypeChoicePermission.maybeResultFor(info, botProfile).flatMap { maybeResult =>
      maybeResult.map(r => Future.successful(Some(r))).getOrElse {
        TextInputPermission.maybeResultFor(info, botProfile).flatMap { maybeResult =>
          maybeResult.map(r => Future.successful(Some(r))).getOrElse {
            YesNoChoicePermission.maybeResultFor(info, botProfile).flatMap { maybeResult =>
              maybeResult.map(r => Future.successful(Some(r))).getOrElse {
                ActionChoicePermission.maybeResultFor(info, botProfile).flatMap { maybeResult =>
                  maybeResult.map(r => Future.successful(Some(r))).getOrElse {
                    HelpIndexPermission.maybeResultFor(info, botProfile).flatMap { maybeResult =>
                      maybeResult.map(r => Future.successful(Some(r))).getOrElse {
                        HelpForSkillPermission.maybeResultFor(info, botProfile).flatMap { maybeResult =>
                          maybeResult.map(r => Future.successful(Some(r))).getOrElse {
                            HelpListAllActionsPermission.maybeResultFor(info, botProfile).flatMap { maybeResult =>
                              maybeResult.map(r => Future.successful(Some(r))).getOrElse {
                                ConfirmContinueConversationPermission.maybeResultFor(info, botProfile).flatMap { maybeResult =>
                                  maybeResult.map(r => Future.successful(Some(r))).getOrElse {
                                    StopConversationPermission.maybeResultFor(info, botProfile).flatMap { maybeResult =>
                                      maybeResult.map(r => Future.successful(Some(r))).getOrElse {
                                        HelpRunBehaviorVersionPermission.maybeResultFor(info, botProfile)
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
                }
              }
            }
          }
        }
      }
    }
  }
}
