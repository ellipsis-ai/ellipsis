package test.integration.services

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}
import services.ElasticsearchService
import org.scalatest.Matchers
import org.scalatest.AsyncFunSpec


trait IntegrationSpec extends AsyncFunSpec with Matchers {
  def appBuilder: GuiceApplicationBuilder = {
    GuiceApplicationBuilder()
  }

  lazy implicit val app: Application = appBuilder.build()
  lazy val configuration = app.injector.instanceOf(classOf[Configuration])
  lazy val elasticsearchService = app.injector.instanceOf(classOf[ElasticsearchService])
}
