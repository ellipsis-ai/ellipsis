package controllers

import java.time.format.DateTimeFormatter
import java.time.{OffsetDateTime, ZoneId}
import javax.inject.{Inject, Singleton}

import controllers.Assets.Asset
import play.api.Configuration
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class RemoteAssets @Inject() (
                             val configuration: Configuration,
                             val assets: Assets,
                             implicit val ec: ExecutionContext
                             )
  extends InjectedController {

  private val timeZoneCode = "GMT"

  private val df: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss '"+timeZoneCode+"'").withLocale(java.util.Locale.ENGLISH).withZone(ZoneId.of(timeZoneCode))

  def getAsset(path: String, file: Asset): Action[AnyContent] = Action.async { request =>
    val action = assets.versioned(path, file)
    action.apply(request).map { result =>
      result.withHeaders(DATE -> df.format(OffsetDateTime.now))
    }
  }

  def getUrl(file: String): String = {
    val asset = Asset(file)
    configuration.getOptional[String]("external_cdn_assets." + asset.name).getOrElse {
      configuration.getOptional[String]("cdn_url") match {
        case Some(contentUrl) => {
          val withoutAssetsPrefix = controllers.routes.RemoteAssets.getAsset(asset).url.substring(7)
          contentUrl + withoutAssetsPrefix
        }
        case None => controllers.routes.RemoteAssets.getAsset(asset).url
      }
    }
  }

}
