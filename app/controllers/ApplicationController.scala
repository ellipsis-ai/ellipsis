package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.Models
import models.accounts.User
import models.bots.conversations.LearnBehaviorConversation
import models.bots.{RegexMessageTriggerQueries, BehaviorParameterQueries, BehaviorQueries}
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

  case class BehaviorParameterData(name: String, question: String)
  case class BehaviorData(
                         id: String,
                         description: String,
                         functionBody: String,
                         params: Seq[BehaviorParameterData],
                         triggers: Seq[String]
                           )

  implicit val behaviorParameterReads: Reads[BehaviorParameterData] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "question").read[String]
    )(BehaviorParameterData.apply _)

  implicit val behaviorParameterWrites: Writes[BehaviorParameterData] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "question").write[String]
    )(unlift(BehaviorParameterData.unapply))

  implicit val behaviorReads: Reads[BehaviorData] = (
    (JsPath \ "behaviorId").read[String] and
      (JsPath \ "description").read[String] and
      (JsPath \ "nodeFunction").read[String] and
      (JsPath \ "params").read[Seq[BehaviorParameterData]] and
      (JsPath \ "triggers").read[Seq[String]]
    )(BehaviorData.apply _)

  implicit val behaviorWrites: Writes[BehaviorData] = (
    (JsPath \ "behaviorId").write[String] and
      (JsPath \ "description").write[String] and
      (JsPath \ "nodeFunction").write[String] and
      (JsPath \ "params").write[Seq[BehaviorParameterData]] and
      (JsPath \ "triggers").write[Seq[String]]
    )(unlift(BehaviorData.unapply))

  def editBehavior(id: String) = SecuredAction.async { implicit request =>
    val action = for {
      maybeBehavior <- BehaviorQueries.find(id)
      maybeParameters <- maybeBehavior.map { behavior =>
        BehaviorParameterQueries.allFor(behavior).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeTriggers <- maybeBehavior.map { behavior =>
        RegexMessageTriggerQueries.allFor(behavior).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      _ <- maybeBehavior.map { behavior =>
        LearnBehaviorConversation.endAllFor(behavior)
      }.getOrElse(DBIO.successful(Unit))
    } yield {
        (for {
          behavior <- maybeBehavior
          params <- maybeParameters
          triggers <- maybeTriggers
        } yield {
          val data = BehaviorData(
            behavior.id,
            behavior.description,
            behavior.functionBody,
            params.map { ea =>
              BehaviorParameterData(ea.name, ea.question)
            },
            triggers.map(ea => ea.regex.pattern.pattern())
          )
          Ok(views.html.edit((Json.toJson(data).toString)))
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
    saveBehaviorForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
      info => {
        val json = Json.parse(info.dataJson)
        json.validate[BehaviorData] match {
          case JsSuccess(data, jsPath) => {
            val action = for {
              maybeBehavior <- BehaviorQueries.find(data.id)
              _ <- maybeBehavior.map { behavior =>
                (for {
                  _ <- DBIO.from(lambdaService.deployFunctionFor(behavior, data.functionBody, BehaviorQueries.withoutBuiltin(data.params.map(_.name).toArray)))
                  _ <- behavior.copy(
                    maybeDescription = Some(data.description),
                    maybeFunctionBody = Some(data.functionBody)
                  ).save
                  _ <- BehaviorParameterQueries.ensureFor(behavior, data.params.map(ea => (ea.name, Some(ea.question))))
                  _ <- RegexMessageTriggerQueries.deleteAllFor(behavior)
                  _ <- DBIO.sequence(
                    data.triggers.
                      filterNot(_.trim.isEmpty).
                      map { trigger =>
                        RegexMessageTriggerQueries.ensureFor(behavior, trigger.r)
                      }
                    )
                } yield Unit) transactionally
              }.getOrElse(DBIO.successful(Unit))
            } yield {
                Redirect(routes.ApplicationController.editBehavior(data.id))
              }

            models.run(action)
          }
          case e: JsError => Future.successful(BadRequest("Malformatted data"))
        }
      }
    )
  }


}
