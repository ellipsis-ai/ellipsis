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

lazy val slackClientVersion = "cd123f514e2be7fa0a7df087197f7cccbba3ca75"
lazy val slackClientProject = ProjectRef(uri(s"https://github.com/ellipsis-ai/slack-scala-client.git#$slackClientVersion"), "slack-scala-client")

libraryDependencies ++= Seq(
  evolutions,
  filters,
  cacheApi,
  guice,
  "com.github.tminglei" %% "slick-pg" % "0.15.3",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.15.3",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.mockito" % "mockito-core" % "1.8.5" % Test,
  "com.mohiva" %% "play-silhouette" % "5.0.0",
  "com.mohiva" %% "play-silhouette-persistence" % "5.0.0",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.0",
  "com.mohiva" %% "play-silhouette-testkit" % "5.0.0" % Test,
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
  "com.typesafe.play" %% "play-slick" % "3.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.1",
  "com.typesafe.play" %% "play-json" % "2.6.3",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "com.amazonaws" % "aws-java-sdk" % "1.11.123",
  "org.webjars" % "requirejs" % "2.2.0",
  "org.webjars.bower" % "core.js" % "2.4.1",
  "org.webjars.bower" % "react" % "15.3.1",
  "org.webjars.bower" % "fetch" % "1.0.0",
  "org.webjars.bower" % "codemirror" % "5.22.2",
  "org.webjars.bower" % "jshint" % "2.9.5",
  "org.webjars.bower" % "javascript-debounce" % "1.0.0",
  "org.webjars.bower" % "moment" % "2.14.1",
  "org.webjars.bower" % "urijs" % "1.18.1",
  "org.webjars.bower" % "jsdiff" % "3.4.0",
  "org.webjars.bower" % "node-uuid" % "1.4.7",
  "com.atlassian.commonmark" % "commonmark" % "0.6.0",
  "com.atlassian.commonmark" % "commonmark-ext-gfm-strikethrough" % "0.6.0",
  "com.atlassian.commonmark" % "commonmark-ext-autolink" % "0.6.0",
  "com.joestelmach" % "natty" % "0.11",
  "com.rockymadden.stringmetric" %% "stringmetric-core" % "0.27.4",
  "org.sangria-graphql" %% "sangria" % "1.2.1",
  "org.sangria-graphql" %% "sangria-play-json" % "1.0.3",
  "com.github.mumoshu" %% "play2-memcached-play26" % "0.9.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
  "io.sentry" % "sentry-logback" % "1.5.6"
)


javaOptions in Test += "-Dconfig.file=conf/test.conf"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

RjsKeys.mainConfig := "build"
RjsKeys.mainModule := "build"
updateOptions := updateOptions.value.withCachedResolution(true)
BabelKeys.options := WebJs.JS.Object(
  "presets" -> List("es2015", "react")
)

scalacOptions in Compile ++= Seq("-Xmax-classfile-name", "128")

fork in run := true
