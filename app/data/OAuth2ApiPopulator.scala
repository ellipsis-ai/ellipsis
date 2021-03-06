package data

import javax.inject._

import models.accounts.oauth2api.{AuthorizationCode, ClientCredentials, OAuth2Api}
import services.DataService

import scala.concurrent.{ExecutionContext, Future}

class OAuth2ApiPopulator @Inject() (
                                     dataService: DataService,
                                     implicit val ec: ExecutionContext
                                   ) {

  val apis: Seq[OAuth2Api] = Seq(
    OAuth2Api(
      "lj8s5CF-QnSY8vtbnWj_BA",
      "Github",
      AuthorizationCode,
      Some("https://github.com/login/oauth/authorize"),
      "https://github.com/login/oauth/access_token",
      Some("https://github.com/settings/applications/new"),
      Some("https://developer.github.com/v3/oauth/#scopes"),
      None,
      None
    ),
    OAuth2Api(
      "Ec2SnSHFTV2Y0jkzbqUmFA",
      "Todoist",
      AuthorizationCode,
      Some("https://todoist.com/oauth/authorize"),
      "https://todoist.com/oauth/access_token",
      Some("https://developer.todoist.com/appconsole.html"),
      Some("https://developer.todoist.com/index.html#oauth"),
      None,
      None
    ),
    OAuth2Api(
      "RdG2Wm5DR0m2_4FZXf-yKA",
      "Google",
      AuthorizationCode,
      Some("https://accounts.google.com/o/oauth2/v2/auth"),
      "https://www.googleapis.com/oauth2/v4/token",
      Some("https://console.developers.google.com/apis"),
      Some("https://developers.google.com/identity/protocols/googlescopes"),
      None,
      None
    ),
    OAuth2Api(
      "0liyxeHPTrqEpRCAsUQJRQ",
      "Yelp",
      ClientCredentials,
      None,
      "https://api.yelp.com/oauth2/token",
      Some("https://www.yelp.com/developers/v3/manage_app"),
      Some("https://www.yelp.com/developers/documentation/v3/get_started"),
      None,
      None
    ),
    OAuth2Api(
      "bu9G_rKuR6mcMZN2iYZ1hg",
      "Jira",
      AuthorizationCode,
      Some("https://auth.atlassian.com/authorize"),
      "https://auth.atlassian.com/oauth/token",
      Some("https://developer.atlassian.com/apps"),
      Some("https://developer.atlassian.com/cloud/jira/platform/scopes/"),
      None,
      Some("api.atlassian.com")
    )
  )

  def run(): Unit = {
    dataService.runNow(Future.sequence(apis.map(dataService.oauth2Apis.save)).map(_ => {}))
  }

  run()
}
