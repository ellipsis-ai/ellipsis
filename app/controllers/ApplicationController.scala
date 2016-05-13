package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.Models
import models.accounts.User
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
      redirectUrl <- configuration.getString("silhouette.slack.redirectURL").map(UriEncoding.encodePathSegment(_, "utf-8"))
    } yield {
        Ok(views.html.addToSlack(request.identity, scopes, clientId, redirectUrl))
      }
    maybeResult.getOrElse(Redirect(routes.ApplicationController.index))
  }

  case class BehaviorParameterData(name: String, question: String)
  case class BehaviorData(
                         id: String,
                         description: String,
                         code: String,
                         params: Seq[BehaviorParameterData],
                         triggers: Seq[String]
                           )

  implicit val behaviorParameterReads: Reads[BehaviorParameterData] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "question").read[String]
    )(BehaviorParameterData.apply _)

  implicit val behaviorReads: Reads[BehaviorData] = (
    (JsPath \ "behaviorId").read[String] and
      (JsPath \ "description").read[String] and
      (JsPath \ "nodeFunction").read[String] and
      (JsPath \ "params").read[Seq[BehaviorParameterData]] and
      (JsPath \ "triggers").read[Seq[String]]
    )(BehaviorData.apply _)

  def editBehavior(id: String) = SecuredAction.async { implicit request =>
    val action = for {
      maybeBehavior <- BehaviorQueries.find(id)
      maybeParameters <- maybeBehavior.map { behavior =>
        BehaviorParameterQueries.allFor(behavior).map(Some(_))
      }.getOrElse(DBIO.successful(None))
      maybeTriggers <- maybeBehavior.map { behavior =>
        RegexMessageTriggerQueries.allFor(behavior).map(Some(_))
      }.getOrElse(DBIO.successful(None))
    } yield {
        (for {
          behavior <- maybeBehavior
          params <- maybeParameters
          triggers <- maybeTriggers
        } yield {
          val json = JsObject(Seq(
            "behaviorId" -> JsString(behavior.id),
            "description" -> JsString(behavior.description),
            "nodeFunction" -> JsString(behavior.code),
            "params" -> JsArray(params.map { ea =>
              JsObject(Seq("name" -> JsString(ea.name), "question" -> JsString(ea.question)))
            }),
            "triggers" -> JsArray(triggers.map(ea => JsString(ea.regex.pattern.pattern())))
          ))
          Ok(views.html.edit(Json.prettyPrint(json)))
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
                  _ <- DBIO.successful(lambdaService.deployFunctionFor(behavior, data.code, BehaviorQueries.withoutBuiltin(data.params.map(_.name).toArray)))
                  _ <- behavior.copy(
                    maybeDescription = Some(data.description),
                    maybeCode = Some(data.code)
                  ).save
                  _ <- BehaviorParameterQueries.ensureFor(behavior, data.params.map(ea => (ea.name, Some(ea.question))))
                  _ <- RegexMessageTriggerQueries.deleteAllFor(behavior)
                  _ <- DBIO.sequence(data.triggers.map { trigger =>
                    RegexMessageTriggerQueries.ensureFor(behavior, trigger.r)
                  })
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
