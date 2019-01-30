import play.sbt.PlayImport.PlayKeys.playRunHooks
import sbt._
import com.typesafe.sbt.web.SbtWeb
import sbt.Keys._

name := """ellipsis"""
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.12"

pipelineStages := Seq(webpack, digest, gzip)

lazy val unitTest = config("ut") extend(Test)
lazy val integrationTest = config("it") extend(Test)


lazy val root = (project in file("."))
    .enablePlugins(PlayScala, SbtWeb)
    .configs(unitTest)
    .settings(inConfig(unitTest)(Defaults.testTasks): _*)
    .settings(testOptions in unitTest := Seq(Tests.Argument("-l", "tags.IntegrationTest")))
    .configs(integrationTest)
    .settings(inConfig(integrationTest)(Defaults.testTasks): _*)
    .settings(testOptions in integrationTest := Seq(Tests.Argument("-n", "tags.IntegrationTest")))


javaOptions in Test += "-Dconfig.file=conf/test.conf"
scalacOptions in Compile ++= Seq("-Xmax-classfile-name", "128")
updateOptions := updateOptions.value.withCachedResolution(true)
fork in run := true

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

libraryDependencies ++= Seq(
  evolutions,
  filters,
  cacheApi,
  guice,
  "com.github.tminglei" %% "slick-pg" % "0.15.3",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.15.3",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % Test,
  "org.mockito" % "mockito-core" % "1.8.5" % Test,
  "com.mohiva" %% "play-silhouette" % "5.0.0",
  "com.mohiva" %% "play-silhouette-persistence" % "5.0.0",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.0",
  "com.mohiva" %% "play-silhouette-testkit" % "5.0.0" % Test,
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
  "com.typesafe.play" %% "play-slick" % "3.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.1",
  "com.typesafe.play" %% "play-json" % "2.6.3",
  "ai.x" %% "play-json-extensions" % "0.10.0",
  "org.postgresql" % "postgresql" % "42.1.1",
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "com.amazonaws" % "aws-java-sdk" % "1.11.488",
  "com.atlassian.commonmark" % "commonmark" % "0.6.0",
  "com.atlassian.commonmark" % "commonmark-ext-gfm-strikethrough" % "0.6.0",
  "com.atlassian.commonmark" % "commonmark-ext-autolink" % "0.6.0",
  "com.joestelmach" % "natty" % "0.11",
  "com.rockymadden.stringmetric" %% "stringmetric-core" % "0.27.4",
  "org.sangria-graphql" %% "sangria" % "1.2.1",
  "org.sangria-graphql" %% "sangria-play-json" % "1.0.3",
  "com.github.mumoshu" %% "play2-memcached-play26" % "0.9.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
  "io.sentry" % "sentry-logback" % "1.7.17",
  "com.chargebee" % "chargebee-java" % "2.3.8",
  "com.typesafe.akka" %% "akka-http-caching" % "10.1.0-RC2",
  "org.eclipse.jgit" % "org.eclipse.jgit" % "5.0.2.201807311906-r",
  "commons-io" % "commons-io" % "2.6"
)

// JavaScript configuration begins
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

val appPath = "./app/assets/frontend"
val targetDir = "target/web/webpack"

// Starts: Webpack server process when running locally and build actions for production bundle
lazy val frontendDirectory = baseDirectory {_ / appPath}
playRunHooks += frontendDirectory.map(base => WebpackServer(base, targetDir)).value
// Ends.
// JavaScript configuration ends
