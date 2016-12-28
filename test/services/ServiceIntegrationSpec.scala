package org.scalatest.examples.integration.services

import org.scalatest.Matchers
import org.scalatest.AsyncFunSpec

import scala.concurrent.Future
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder
import services.ElasticsearchService


trait ServiceIntegrationSpec extends AsyncFunSpec with Matchers {
  def appBuilder: GuiceApplicationBuilder = {
    GuiceApplicationBuilder()
  }

  lazy implicit val app: Application = appBuilder.build()
  lazy val configuration = app.injector.instanceOf(classOf[Configuration])
  lazy val elasticsearchService = app.injector.instanceOf(classOf[ElasticsearchService])
}
