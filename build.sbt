import WebJs._
import RjsKeys._

name := """ellipsis"""

version := "1.0-SNAPSHOT"

lazy val root =
  (project in file(".")).
    dependsOn(slackClientProject).
    enablePlugins(PlayScala, SbtWeb)

pipelineStages := Seq(rjs, digest, gzip)

scalaVersion := "2.11.8"

lazy val slackClientVersion = "3feb51c21462b9d677b6cf5ebce9ad8b2fc4c3aa"
lazy val slackClientProject = ProjectRef(uri(s"https://github.com/ellipsis-ai/slack-scala-client.git#$slackClientVersion"), "slack-scala-client")

libraryDependencies ++= Seq(
  evolutions,
  jdbc,
  filters,
  cache,
//  "com.github.gilbertw1" %% "slack-scala-client" % "0.1.8",
  "com.github.nscala-time" %% "nscala-time" % "2.14.0",
  "com.typesafe.slick" % "slick-hikaricp_2.11" % "3.1.1",
  "com.github.tminglei" %% "slick-pg" % "0.14.4",
  "com.github.tminglei" %% "slick-pg_date2" % "0.14.4",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.14.4",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.mockito" % "mockito-core" % "1.8.5" % Test,
  "com.mohiva" %% "play-silhouette" % "4.0.0",
  "com.mohiva" %% "play-silhouette-persistence" % "4.0.0",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "4.0.0",
  "com.mohiva" %% "play-silhouette-testkit" % "4.0.0" % Test,
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "com.amazonaws" % "aws-java-sdk" % "1.10.72",
  "org.webjars" % "requirejs" % "2.2.0",
  "org.webjars.bower" % "core.js" % "2.4.1",
  "org.webjars.bower" % "react" % "15.3.1",
  "org.webjars.bower" % "fetch" % "1.0.0",
  "org.webjars.bower" % "codemirror" % "5.22.2",
  "org.webjars.bower" % "jshint" % "2.9.2",
  "org.webjars.bower" % "javascript-debounce" % "1.0.0",
  "org.webjars.bower" % "moment" % "2.14.1",
  "org.webjars.bower" % "urijs" % "1.18.1",
  "com.atlassian.commonmark" % "commonmark" % "0.6.0",
  "com.atlassian.commonmark" % "commonmark-ext-gfm-strikethrough" % "0.6.0",
  "com.atlassian.commonmark" % "commonmark-ext-autolink" % "0.6.0",
  "com.joestelmach" % "natty" % "0.11",
  "com.rockymadden.stringmetric" %% "stringmetric-core" % "0.27.4"
  //"com.github.mumoshu" %% "play2-memcached-play24" % "0.7.0",
  "wabisabi" %% "wabisabi" % "2.1.0"
)
resolvers += "gphat" at "https://raw.github.com/gphat/mvn-repo/master/releases/"
javaOptions in Test += "-Dconfig.file=conf/test.conf"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

RjsKeys.mainConfig := "build"
RjsKeys.mainModule := "build"
updateOptions := updateOptions.value.withCachedResolution(true)
BabelKeys.options := WebJs.JS.Object(
  "presets" -> List("react", "es2015")
)


fork in run := true
