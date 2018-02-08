import com.typesafe.sbt.web.pipeline.Pipeline
import play.sbt.PlayImport.PlayKeys.playRunHooks
import sbt._
import com.typesafe.sbt.web.SbtWeb
import sbt.Keys._

name := """ellipsis"""
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.8"

pipelineStages := Seq(webpackBuild, rjs, digest, gzip)

lazy val slackClientVersion = "cd123f514e2be7fa0a7df087197f7cccbba3ca75"
lazy val slackClientProject = ProjectRef(uri(s"https://github.com/ellipsis-ai/slack-scala-client.git#$slackClientVersion"), "slack-scala-client")

lazy val root =
  (project in file(".")).
    dependsOn(slackClientProject).
    enablePlugins(PlayScala, SbtWeb)

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
  "org.webjars.npm" % "little-loader" % "0.2.0",
  "com.atlassian.commonmark" % "commonmark" % "0.6.0",
  "com.atlassian.commonmark" % "commonmark-ext-gfm-strikethrough" % "0.6.0",
  "com.atlassian.commonmark" % "commonmark-ext-autolink" % "0.6.0",
  "com.joestelmach" % "natty" % "0.11",
  "com.rockymadden.stringmetric" %% "stringmetric-core" % "0.27.4",
  "org.sangria-graphql" %% "sangria" % "1.2.1",
  "org.sangria-graphql" %% "sangria-play-json" % "1.0.3",
  "com.github.mumoshu" %% "play2-memcached-play26" % "0.9.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
  "io.sentry" % "sentry-logback" % "1.5.6",
  "com.chargebee" % "chargebee-java" % "2.3.8"
)

// JavaScript configuration begins
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

// Starts: Webpack build task
val appPath = "./app/assets/frontend"
val targetDir = "target/web/webpack"
val assetDir = "bundles"
val webpackBuild = taskKey[Pipeline.Stage]("Webpack build task.")

webpackBuild := { mappings =>
  Process("npm run build", file(appPath), ("WEBPACK_BUILD_PATH", targetDir)).!
  val files = IO.listFiles(file(s"./$targetDir"))
  val newMappings = files.map(file => {
    println(file.getPath)
    (file, file.getPath.replace(targetDir, assetDir))
  })
  mappings ++ newMappings
}

packageBin in Universal := (packageBin in Universal).dependsOn(webpackBuild).value
webpackBuild in Assets := (webpackBuild in Assets).dependsOn(WebKeys.webModules in Assets).value
dist := (dist dependsOn webpackBuild).value
stage := (stage dependsOn webpackBuild).value
// Ends.

// Starts: Webpack server process when running locally and build actions for production bundle
lazy val frontendDirectory = baseDirectory {_ / appPath}
playRunHooks += frontendDirectory.map(base => WebpackServer(base, targetDir)).value
// Ends.
// JavaScript configuration ends
