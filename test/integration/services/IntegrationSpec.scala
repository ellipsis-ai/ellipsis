package test.integration.services

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}
import services.ElasticsearchService
import org.scalatestplus.play.PlaySpec


trait IntegrationSpec extends PlaySpec {
  def appBuilder: GuiceApplicationBuilder = {
    GuiceApplicationBuilder()
  }

  lazy implicit val app: Application = appBuilder.build()
  lazy val configuration = app.injector.instanceOf(classOf[Configuration])
  lazy val elasticsearchService = app.injector.instanceOf(classOf[ElasticsearchService])
}
