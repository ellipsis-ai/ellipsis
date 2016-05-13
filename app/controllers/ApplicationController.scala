package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.Models
import models.accounts.User
import models.bots.{RegexMessageTriggerQueries, BehaviorParameterQueries, BehaviorQueries}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.{Json, JsArray, JsString, JsObject}
import play.utils.UriEncoding
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationController @Inject() (
                                        val messagesApi: MessagesApi,
                                        val env: Environment[User, CookieAuthenticator],
                                        val configuration: Configuration,
                                        val models: Models,
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
            "description" -> JsString(behavior.description),
            "nodeFunction" -> JsString(behavior.code),
            "params" -> JsArray(params.map { ea =>
              JsObject(Seq("name" -> JsString(ea.name), "question" -> JsString(ea.question)))
            }),
            "triggers" -> JsArray(triggers.map(ea => JsString(ea.regex.pattern.pattern()))),
            "regexTrigger" -> JsString("")
          ))
          Ok(views.html.edit(Json.prettyPrint(json)))
        }).getOrElse {
          NotFound("Behavior not found")
        }
      }

    models.run(action)
  }

}
