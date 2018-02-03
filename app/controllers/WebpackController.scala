package controllers

import javax.inject._

import com.google.inject.Provider
import controllers.Assets.Asset

import scala.concurrent.ExecutionContext.Implicits.global
import play.api._
import play.api.libs.ws._
import play.api.mvc._

import scala.concurrent.Future

class WebpackController @Inject()(
                                   val ws: WSClient,
                                   val environment: Environment,
                                   val configuration: Configuration,
                                   val assetsProvider: Provider[RemoteAssets]
                                 ) extends EllipsisController {
  def bundle(file: String): Action[AnyContent] = Action.async {
    // In dev mode, proxy to the node.js webpack dev server
    if (environment.mode == Mode.Dev) {
      val host = configuration.get[String]("webpack.devServer.host")
      val port = configuration.get[String]("webpack.devServer.port")
      val url = s"http://$host:$port/bundles/$file"
      ws.url(url).get().map { response =>
        val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("application/octet-stream")
        val headers = response.headers
          .toSeq.filter(p => List("Content-Type", "Content-Length").indexOf(p._1) < 0).map(p => (p._1, p._2.mkString))
        Ok(response.body).withHeaders(headers: _*).as(contentType)
      }
    } else {
      val asset = Asset(s"/bundles/$file")
      Future.successful(Redirect(routes.RemoteAssets.getAsset(asset)))
    }
  }
}
