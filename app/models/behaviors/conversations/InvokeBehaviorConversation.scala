package models.behaviors.conversations

import models.IDs
import models.accounts.user.User
import models.behaviors._
import models.behaviors.behaviorparameter.{BehaviorParameter, BehaviorParameterContext}
import models.behaviors.behaviorversion.BehaviorVersion
import models.behaviors.conversations.collectedparametervalue.CollectedParameterValue
import models.behaviors.conversations.conversation.Conversation
import models.behaviors.events.MessageEvent
import models.behaviors.triggers.messagetrigger.MessageTrigger
import org.joda.time.DateTime
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.ws.WSClient
import services.{AWSLambdaConstants, AWSLambdaService, DataService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class InvokeBehaviorConversation(
                                      id: String,
                                      trigger: MessageTrigger,
                                      context: String, // Slack, etc
                                      userIdForContext: String, // id for Slack, etc user
                                      startedAt: DateTime,
                                      state: String = Conversation.NEW_STATE
                                      ) extends Conversation {

  val conversationType = Conversation.INVOKE_BEHAVIOR

  override val stateRequiresPrivateMessage: Boolean = state == InvokeBehaviorConversation.COLLECT_USER_ENV_VARS_STATE

  def updateStateTo(newState: String, dataService: DataService): Future[Conversation] = {
    dataService.conversations.save(this.copy(state = newState))
  }

  def updateToNextState(event: MessageEvent, cache: CacheApi, dataService: DataService, configuration: Configuration): Future[Conversation] = {
    for {
      user <- event.context.ensureUser(dataService)
      needsUserEnvVar <- userEnvVarInfo(user, dataService).flatMap { info =>
        info.maybeNextToCollect.map(_.isDefined)
      }
      needsParam <- paramInfo(dataService).flatMap { info =>
        info.maybeNextToCollect(event, this, cache, dataService, configuration).map(_.isDefined)
      }
      updated <- {
        val targetState = if (needsUserEnvVar) {
          InvokeBehaviorConversation.COLLECT_USER_ENV_VARS_STATE
        } else if (needsParam) {
          InvokeBehaviorConversation.COLLECT_PARAM_VALUES_STATE
        } else {
          Conversation.DONE_STATE
        }
        updateStateTo(targetState, dataService)
      }
    } yield updated
  }

  case class ParamInfo(params: Seq[BehaviorParameter], collected: Seq[CollectedParameterValue]) {

    val rankedParams = params.sortBy(_.rank)

    def maybeNextToCollect(
                            event: MessageEvent,
                            conversation: Conversation,
                            cache: CacheApi,
                            dataService: DataService,
                            configuration: Configuration
                          ): Future[Option[(BehaviorParameter, Option[CollectedParameterValue])]] = {
      val tuples = rankedParams.map { ea => (ea, collected.find(_.parameter == ea)) }

      val eventualWithHasValidValue = Future.sequence(tuples.map { case(param, maybeCollected) =>
        val context = BehaviorParameterContext(event, Some(conversation), param, cache, dataService, configuration)
        val eventualHasValidValue = maybeCollected.map { collected =>
          param.paramType.isValid(collected.valueString, context)
        }.getOrElse(Future.successful(false))

        eventualHasValidValue.map { hasValidValue =>
          (param, maybeCollected, hasValidValue)
        }
      })

      eventualWithHasValidValue.map { withHasValidValue =>
        withHasValidValue.
          find { case (param, maybeCollected, hasValidValue) => !hasValidValue }.
          map { case (param, maybeCollected, hasValidValue) => (param, maybeCollected) }
      }
    }

    def invocationMap: Map[String, String] = {
      rankedParams.zipWithIndex.map { case(ea, i) =>
        val maybeParamValue = collected.find(_.parameter.id == ea.id).map(_.valueString)
        (AWSLambdaConstants.invocationParamFor(i), maybeParamValue.getOrElse(""))
      }.toMap
    }
  }

  private def paramInfo(dataService: DataService): Future[ParamInfo] = {
    for {
      params <- dataService.behaviorParameters.allFor(behaviorVersion)
      collected <- dataService.collectedParameterValues.allFor(this)
    } yield ParamInfo(params, collected)
  }

  case class UserEnvVarInfo(missingEnvVarNames: Seq[String]) {

    val sortedEnvVars = missingEnvVarNames.sorted

    def maybeNextToCollect: Future[Option[String]] = {
      Future.successful(sortedEnvVars.headOption)
    }
  }

  private def userEnvVarInfo(user: User, dataService: DataService): Future[UserEnvVarInfo] = {
    for {
      missingUserEnvVars <- dataService.userEnvironmentVariables.missingFor(user, behaviorVersion, dataService)
    } yield UserEnvVarInfo(missingUserEnvVars)
  }

  private def collectParamValueFrom(event: MessageEvent, info: ParamInfo, dataService: DataService, cache: CacheApi, configuration: Configuration): Future[Conversation] = {
    for {
      maybeNextToCollect <- info.maybeNextToCollect(event, this, cache, dataService, configuration)
      updatedConversation <- maybeNextToCollect.map { case(param, maybeCollected) =>
        val context = BehaviorParameterContext(event, Some(this), param, cache, dataService, configuration)
        param.paramType.handleCollected(event, context).map(_ => this)
      }.getOrElse(Future.successful(this))
      updatedConversation <- updatedConversation.updateToNextState(event, cache, dataService, configuration)
    } yield updatedConversation
  }

  private def collectUserEnvVarFrom(event: MessageEvent, info: UserEnvVarInfo, dataService: DataService, cache: CacheApi, configuration: Configuration): Future[Conversation] = {
    for {
      maybeNextToCollect <- info.maybeNextToCollect
      user <- event.context.ensureUser(dataService)
      updatedConversation <- maybeNextToCollect.map { envVarName =>
        dataService.userEnvironmentVariables.ensureFor(envVarName, Some(event.context.relevantMessageText), user).map(_ => this)
      }.getOrElse(Future.successful(this))
      updatedConversation <- updatedConversation.updateToNextState(event, cache, dataService, configuration)
    } yield updatedConversation
  }

  def updateWith(event: MessageEvent, lambdaService: AWSLambdaService, dataService: DataService, cache: CacheApi, configuration: Configuration): Future[Conversation] = {
    import Conversation._
    import InvokeBehaviorConversation._

    for {
      user <- event.context.ensureUser(dataService)
      userEnvVarInfo <- userEnvVarInfo(user, dataService)
      paramInfo <- paramInfo(dataService)
      updated <- state match {
        case NEW_STATE => updateToNextState(event, cache, dataService, configuration)
        case COLLECT_USER_ENV_VARS_STATE => collectUserEnvVarFrom(event, userEnvVarInfo, dataService, cache, configuration)
        case COLLECT_PARAM_VALUES_STATE => collectParamValueFrom(event, paramInfo, dataService, cache, configuration)
        case DONE_STATE => Future.successful(this)
      }
    } yield updated
  }

  private def promptResultFor(info: ParamInfo, event: MessageEvent, dataService: DataService, cache: CacheApi, configuration: Configuration): Future[BotResult] = {
    for {
      maybeNextToCollect <- info.maybeNextToCollect(event, this, cache, dataService, configuration)
      result <- maybeNextToCollect.map { case(param, maybeCollected) =>
        val context = BehaviorParameterContext(event, Some(this), param, cache, dataService, configuration)
        param.prompt(maybeCollected, context)
      }.getOrElse {
        Future.successful("All done!")
      }.map { prompt =>
        SimpleTextResult(prompt, behaviorVersion.forcePrivateResponse)
      }
    } yield result
  }

  private def promptResultFor(info: UserEnvVarInfo, event: MessageEvent, dataService: DataService, cache: CacheApi, configuration: Configuration): Future[BotResult] = {
    info.maybeNextToCollect.map { maybeNextToCollect =>
      val prompt = maybeNextToCollect.map { envVarName =>
        s"To run this skill, I first need a value for $envVarName. This is specific to you and I'll only ask for it once"
      }.getOrElse {
        "All done!"
      }
      SimpleTextResult(prompt, forcePrivateResponse = true)
    }
  }

  def respond(
               event: MessageEvent,
               lambdaService: AWSLambdaService,
               dataService: DataService,
               cache: CacheApi,
               ws: WSClient,
               configuration: Configuration
             ): Future[BotResult] = {
    import Conversation._
    import InvokeBehaviorConversation._

    for {
      user <- event.context.ensureUser(dataService)
      userEnvVarInfo <- userEnvVarInfo(user, dataService)
      paramInfo <- paramInfo(dataService)
      result <- state match {
        case COLLECT_PARAM_VALUES_STATE => promptResultFor(paramInfo, event, dataService, cache, configuration)
        case COLLECT_USER_ENV_VARS_STATE => promptResultFor(userEnvVarInfo, event, dataService, cache, configuration)
        case DONE_STATE => {
          BehaviorResponse.buildFor(event, behaviorVersion, paramInfo.invocationMap, trigger, Some(this), lambdaService, dataService, cache, ws, configuration).flatMap { br =>
            br.resultForFilledOut
          }
        }
      }
    } yield result
  }

}

object InvokeBehaviorConversation {

  val COLLECT_USER_ENV_VARS_STATE = "collect_user_env_vars"
  val COLLECT_PARAM_VALUES_STATE = "collect_param_values"

  def createFor(
                 behaviorVersion: BehaviorVersion,
                 context: String,
                 userIdForContext: String,
                 activatedTrigger: MessageTrigger,
                 dataService: DataService
                 ): Future[InvokeBehaviorConversation] = {
    val newInstance =
      InvokeBehaviorConversation(
        IDs.next,
        activatedTrigger,
        context,
        userIdForContext,
        DateTime.now,
        Conversation.NEW_STATE
      )
    dataService.conversations.save(newInstance).map(_ => newInstance)
  }
}
