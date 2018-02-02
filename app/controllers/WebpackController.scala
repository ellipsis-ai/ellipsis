package controllers

import javax.inject._

import scala.concurrent.ExecutionContext.Implicits.global
import play.api._
import play.api.libs.ws._
import play.api.mvc._

class WebpackController @Inject()(
                                   ws: WSClient,
                                   assets: Assets,
                                   environment: Environment,
                                   configuration: Configuration,
                                   cc: ControllerComponents
                                 ) extends AbstractController(cc) {
  def bundle(file:String): Action[AnyContent] = if (environment.mode == Mode.Dev) Action.async {
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
    assets.at("/public/bundles", file)
  }
}
