package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.bots.triggers.MessageTriggerQueries
import models.{Team, EnvironmentVariableQueries, Models}
import models.accounts.User
import models.bots.{BehaviorQueries, BehaviorParameterQueries, BehaviorVersionQueries}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.utils.UriEncoding
import services.AWSLambdaService
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationController @Inject() (
                                        val messagesApi: MessagesApi,
                                        val env: Environment[User, CookieAuthenticator],
                                        val configuration: Configuration,
                                        val models: Models,
                                        val lambdaService: AWSLambdaService,
                                        socialProviderRegistry: SocialProviderRegistry)
  extends Silhouette[User, CookieAuthenticator] {

  def index = SecuredAction { implicit request => Ok(views.html.yay()) }

  def addToSlack = UserAwareAction { implicit request =>
    val maybeResult = for {
      scopes <- configuration.getString("silhouette.slack.scope")
      clientId <- configuration.getString("silhouette.slack.clientID")
    } yield {
        val redirectUrl = routes.SocialAuthController.installForSlack().absoluteURL(secure=true)
        Ok(views.html.addToSlack(request.identity, scopes, clientId, redirectUrl))
      }
    maybeResult.getOrElse(Redirect(routes.ApplicationController.index))
  }

  def signInWithSlack(maybeRedirectUrl: Option[String]) = UserAwareAction { implicit request =>
    val maybeResult = for {
      scopes <- configuration.getString("silhouette.slack.signInScope")
      clientId <- configuration.getString("silhouette.slack.clientID")
    } yield {
        val redirectUrl = routes.SocialAuthController.authenticateSlack(maybeRedirectUrl.map(UriEncoding.encodePathSegment(_, "utf-8"))).absoluteURL(secure=true)
        Ok(views.html.signInWithSlack(request.identity, scopes, clientId, redirectUrl))
      }
    maybeResult.getOrElse(Redirect(routes.ApplicationController.index))
  }

  def newBehavior(teamId: String) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeTeam <- Team.find(teamId, user)
      maybeEnvironmentVariables <- maybeTeam.map { team =>
        EnvironmentVariableQueries.allFor(team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
        (for {
          team <- maybeTeam
          envVars <- maybeEnvironmentVariables
        } yield {
            val data = BehaviorVersionData(
              team.id,
              None,
              "",
              "",
              Seq(),
              Seq(),
              None
            )
            Ok(views.html.edit(Json.toJson(data).toString, envVars.map(_.name), justSaved = false))
          }).getOrElse {
          Redirect(routes.ApplicationController.signInWithSlack(Some(request.uri)))
        }
      }

    models.run(action)
  }

  case class BehaviorParameterData(name: String, question: String)
  case class BehaviorTriggerData(
                                  text: String,
                                  requiresMention: Boolean,
                                  isRegex: Boolean,
                                  caseSensitive: Boolean
                                )

  case class BehaviorVersionData(
                         teamId: String,
                         maybeId: Option[String],
                         functionBody: String,
                         responseTemplate: String,
                         params: Seq[BehaviorParameterData],
                         triggers: Seq[BehaviorTriggerData],
                         maybeCreatedAt: Option[DateTime]
                           )

  case class BehaviorData(behaviorId: String, versions: Seq[BehaviorVersionData])

  implicit val behaviorParameterReads: Reads[BehaviorParameterData] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "question").read[String]
    )(BehaviorParameterData.apply _)

  implicit val behaviorParameterWrites: Writes[BehaviorParameterData] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "question").write[String]
    )(unlift(BehaviorParameterData.unapply))

  implicit val behaviorTriggerReads: Reads[BehaviorTriggerData] = (
    (JsPath \ "text").read[String] and
      (JsPath \ "requiresMention").read[Boolean] and
      (JsPath \ "isRegex").read[Boolean] and
      (JsPath \ "caseSensitive").read[Boolean]
    )(BehaviorTriggerData.apply _)

  implicit val behaviorTriggerWrites: Writes[BehaviorTriggerData] = (
    (JsPath \ "text").write[String] and
      (JsPath \ "requiresMention").write[Boolean] and
      (JsPath \ "isRegex").write[Boolean] and
      (JsPath \ "caseSensitive").write[Boolean]
    )(unlift(BehaviorTriggerData.unapply))

  implicit val behaviorVersionReads: Reads[BehaviorVersionData] = (
    (JsPath \ "teamId").read[String] and
      (JsPath \ "behaviorId").readNullable[String] and
      (JsPath \ "nodeFunction").read[String] and
      (JsPath \ "responseTemplate").read[String] and
      (JsPath \ "params").read[Seq[BehaviorParameterData]] and
      (JsPath \ "triggers").read[Seq[BehaviorTriggerData]] and
      (JsPath \ "createdAt").readNullable[DateTime]
    )(BehaviorVersionData.apply _)

  implicit val behaviorVersionWrites: Writes[BehaviorVersionData] = (
    (JsPath \ "teamId").write[String] and
      (JsPath \ "behaviorId").writeNullable[String] and
      (JsPath \ "nodeFunction").write[String] and
      (JsPath \ "responseTemplate").write[String] and
      (JsPath \ "params").write[Seq[BehaviorParameterData]] and
      (JsPath \ "triggers").write[Seq[BehaviorTriggerData]] and
      (JsPath \ "createdAt").writeNullable[DateTime]
    )(unlift(BehaviorVersionData.unapply))

  implicit val behaviorReads: Reads[BehaviorData] = (
    (JsPath \ "behaviorId").read[String] and
      (JsPath \ "versions").read[Seq[BehaviorVersionData]]
    )(BehaviorData.apply _)

  implicit val behaviorWrites: Writes[BehaviorData] = (
    (JsPath \ "behaviorId").write[String] and
      (JsPath \ "versions").write[Seq[BehaviorVersionData]]
    )(unlift(BehaviorData.unapply))

  def editBehavior(id: String, maybeJustSaved: Option[Boolean]) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeBehavior <- BehaviorQueries.find(id, user)
      maybeBehaviorVersion <- maybeBehavior.map { behavior =>
        behavior.maybeCurrentVersion
      }.getOrElse(DBIO.successful(None))
      maybeParameters <- maybeBehaviorVersion.map { behaviorVersion =>
        BehaviorParameterQueries.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeTriggers <- maybeBehaviorVersion.map { behaviorVersion =>
        MessageTriggerQueries.allFor(behaviorVersion).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeEnvironmentVariables <- maybeBehaviorVersion.map { behaviorVersion =>
        EnvironmentVariableQueries.allFor(behaviorVersion.team).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
        (for {
          behavior <- maybeBehavior
          behaviorVersion <- maybeBehaviorVersion
          params <- maybeParameters
          triggers <- maybeTriggers
          envVars <- maybeEnvironmentVariables
        } yield {
          val data = BehaviorVersionData(
            behaviorVersion.team.id,
            Some(behavior.id),
            behaviorVersion.functionBody,
            behaviorVersion.maybeResponseTemplate.getOrElse(""),
            params.map { ea =>
              BehaviorParameterData(ea.name, ea.question)
            },
            triggers.map( ea =>
              BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
            ),
            Some(behaviorVersion.createdAt)
          )
          Ok(views.html.edit(Json.toJson(data).toString, envVars.map(_.name), maybeJustSaved.exists(identity)))
        }).getOrElse {
          NotFound("Behavior not found")
        }
      }

    models.run(action)
  }

  case class SaveBehaviorInfo(dataJson: String)

  private val saveBehaviorForm = Form(
    mapping(
      "dataJson" -> nonEmptyText
    )(SaveBehaviorInfo.apply)(SaveBehaviorInfo.unapply)
  )

  def saveBehavior = SecuredAction.async { implicit request =>
    val user = request.identity
    saveBehaviorForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[BehaviorVersionData] match {
          case JsSuccess(data, jsPath) => {
            val action = (for {
              maybeTeam <- Team.find(data.teamId, user)
              maybeBehavior <- data.maybeId.map { behaviorId =>
                BehaviorQueries.find(behaviorId, user)
              }.getOrElse {
                maybeTeam.map { team =>
                  BehaviorQueries.createFor(team).map(Some(_))
                }.getOrElse(DBIO.successful(None))
              }
              maybeBehaviorVersion <- maybeBehavior.map { behavior =>
                BehaviorVersionQueries.createFor(behavior).map(Some(_))
              }.getOrElse(DBIO.successful(None))
              _ <- maybeBehaviorVersion.map { behaviorVersion =>
                for {
                  _ <- DBIO.from(lambdaService.deployFunctionFor(behaviorVersion, data.functionBody, BehaviorVersionQueries.withoutBuiltin(data.params.map(_.name).toArray)))
                  updated <- behaviorVersion.copy(
                    maybeFunctionBody = Some(data.functionBody),
                    maybeResponseTemplate = Some(data.responseTemplate)
                  ).save
                  _ <- BehaviorParameterQueries.ensureFor(behaviorVersion, data.params.map(ea => (ea.name, Some(ea.question))))
                  _ <- DBIO.sequence(
                    data.triggers.
                      filterNot(_.text.trim.isEmpty)
                      map { trigger =>
                        MessageTriggerQueries.createFor(behaviorVersion, trigger.text, trigger.requiresMention, trigger.isRegex, trigger.caseSensitive)
                      }
                    )
                } yield Unit
              }.getOrElse(DBIO.successful(Unit))
            } yield {
                maybeBehavior.map { behavior =>
                  Redirect(routes.ApplicationController.editBehavior(behavior.id, justSaved = Some(true)))
                }.getOrElse {
                  NotFound("Behavior not found")
                }
              }) transactionally

            models.run(action)
          }
          case e: JsError => Future.successful(BadRequest("Malformatted data"))
        }
      }
    )
  }

  private val deleteBehaviorForm = Form(
    "behaviorId" -> nonEmptyText
  )

  def deleteBehavior = SecuredAction.async { implicit request =>
    deleteBehaviorForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      behaviorId => {
        val action = for {
          maybeBehavior <- BehaviorQueries.find(behaviorId, request.identity)
          _ <- maybeBehavior.map { behavior =>
            behavior.unlearn(lambdaService)
          }.getOrElse(DBIO.successful(Unit))
        } yield Redirect(routes.ApplicationController.index())

        models.run(action)
      }
    )
  }

  def versionInfoFor(behaviorId: String) = SecuredAction.async { implicit request =>
    val user = request.identity
    val action = for {
      maybeBehavior <- BehaviorQueries.find(behaviorId, user)
      versions <- maybeBehavior.map { behavior =>
        BehaviorVersionQueries.allFor(behavior)
      }.getOrElse(DBIO.successful(Seq()))
      parametersByVersion <- DBIO.sequence(versions.map { version =>
        BehaviorParameterQueries.allFor(version).map { params =>
          (version, params)
        }
      }).map(_.toMap)
      triggersByVersion <- DBIO.sequence(versions.map { version =>
        MessageTriggerQueries.allFor(version).map { triggers =>
          (version, triggers)
        }
      }).map(_.toMap)
    } yield {
        maybeBehavior.map { behavior =>
          val versionsData = versions.map { version =>
            BehaviorVersionData(
              version.team.id,
              Some(behavior.id),
              version.functionBody,
              version.maybeResponseTemplate.getOrElse(""),
              parametersByVersion.get(version).map { params =>
                params.map { ea =>
                  BehaviorParameterData(ea.name, ea.question)
                }
              }.getOrElse(Seq()),
              triggersByVersion.get(version).map { triggers =>
                triggers.map { ea =>
                  BehaviorTriggerData(ea.pattern, requiresMention = ea.requiresBotMention, isRegex = ea.shouldTreatAsRegex, caseSensitive = ea.isCaseSensitive)
                }
              }.getOrElse(Seq()),
              Some(version.createdAt)
            )
          }
          Ok(Json.toJson(versionsData))
        }.getOrElse {
          NotFound("Behavior not found")
        }
      }

    models.run(action)
  }

  def regexValidationErrorsFor(pattern: String) = SecuredAction { implicit request =>
    val content = MessageTriggerQueries.maybeRegexValidationErrorFor(pattern).map { errMessage =>
      Array(errMessage)
    }.getOrElse {
      Array()
    }
    Ok(Json.toJson(Array(content)))
  }

}
