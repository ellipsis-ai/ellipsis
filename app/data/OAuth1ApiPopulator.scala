package data

import javax.inject._
import models.accounts.oauth1api.OAuth1Api
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class OAuth1ApiPopulator @Inject() (
                                     dataService: DataService,
                                     implicit val ec: ExecutionContext
                                   ) {

  val apis: Seq[OAuth1Api] = Seq(
    OAuth1Api(
      "7gK5ysNxSjSa9BzfB44yAg",
      "Trello",
      "https://trello.com/1/OAuthGetRequestToken",
      "https://trello.com/1/OAuthGetAccessToken",
      "https://trello.com/1/OAuthAuthorizeToken",
      Some("https://trello.com/app-key"),
      None,
      None
    )
  )

  def run(): Unit = {
    dataService.runNow(Future.sequence(apis.map(dataService.oauth1Apis.save)).map(_ => {}))
  }

  run()
}
