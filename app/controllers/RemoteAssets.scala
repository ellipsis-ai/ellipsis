package controllers

import controllers.Assets.Asset
import play.api.mvc._
import play.api.Play
import play.api.Play.current
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.DateTimeZone

import scala.concurrent.ExecutionContext.Implicits.global

class RemoteAssets extends Controller {

  private val timeZoneCode = "GMT"

  private val df: DateTimeFormatter =
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '"+timeZoneCode+"'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))

  type ResultWithHeaders = Result { def withHeaders(headers: (String, String)*): Result }

  def getAsset(path: String, file: Asset): Action[AnyContent] = Action.async { request =>
    val action = Assets.versioned(path, file)
    action.apply(request).map { result =>
      val resultWithHeaders = result.asInstanceOf[ResultWithHeaders]
      resultWithHeaders.withHeaders(DATE -> df.print((new java.util.Date).getTime))
    }
  }

}

object RemoteAssets {

  def getUrl(file: String): String = {
    val asset = Asset(file)
    Play.configuration.getString("external_cdn_assets." + asset.name).getOrElse {
      Play.configuration.getString("cdn_url") match {
        case Some(contentUrl) => {
          val withoutAssetsPrefix = controllers.routes.RemoteAssets.getAsset(asset).url.substring(7)
          contentUrl + withoutAssetsPrefix
        }
        case None => controllers.routes.RemoteAssets.getAsset(asset).url
      }
    }
  }

}
