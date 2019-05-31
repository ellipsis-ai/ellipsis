package controllers

import java.time.OffsetDateTime
import java.time.format.TextStyle
import java.util.Locale

import javax.inject.Inject
import com.google.inject.Provider
import com.mohiva.play.silhouette.api.Silhouette
import json._
import models.silhouette.EllipsisEnv
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.filters.csrf.CSRF
import services.{DefaultServices, GithubService}
import utils.github.{GithubPublishedBehaviorGroupsFetcher, GithubSingleCommitFetcher, GithubSkillCommitsFetcher}
import utils.{CitiesToTimeZones, FuzzyMatcher, TimeZoneParser}

import scala.concurrent.{ExecutionContext, Future}

class FormController @Inject() (
                                  val silhouette: Silhouette[EllipsisEnv],
                                  val services: DefaultServices,
                                  val assetsProvider: Provider[RemoteAssets],
                                  implicit val ec: ExecutionContext
                                ) extends ReAuthable {

  import json.Formatting._

  val configuration = services.configuration
  val dataService = services.dataService
  val lambdaService = services.lambdaService
  val cacheService = services.cacheService
  val ws = services.ws

  def form(formId: String) = silhouette.SecuredAction.async { implicit request =>
    val user = request.identity
    render.async {
      case Accepts.JavaScript() => {
        Future.successful(Ok(""))
      }
      case Accepts.Html() => {
        for {
          maybeForm <- dataService.forms.find(formId)
        } yield maybeForm.map { form =>
          Ok(form.id)
        }.getOrElse(NotFound(""))
      }
    }
  }
}
