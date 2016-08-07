import WebJs._
import RjsKeys._

name := """ellipsis"""

version := "1.0-SNAPSHOT"

lazy val root =
  (project in file(".")).
    dependsOn(slackClientProject).
    enablePlugins(PlayScala, SbtWeb)

pipelineStages := Seq(rjs, digest, gzip)

scalaVersion := "2.11.7"

lazy val slackClientVersion = "07dff82bb109509aa093651f85e3ba9720834f45"

lazy val slackClientProject =
  RootProject(uri(s"https://github.com/ellipsis-ai/slack-scala-client.git#$slackClientVersion"))

libraryDependencies ++= Seq(
  evolutions,
  jdbc,
  filters,
  cache,
  "com.zaxxer" % "HikariCP" % "2.4.1",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0",
  "org.scalatestplus" % "play_2.11" % "1.4.0" % Test,
  "com.mohiva" %% "play-silhouette" % "3.0.2",
  "com.typesafe.slick" %% "slick" % "3.0.0",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "com.amazonaws" % "aws-java-sdk" % "1.10.72",
  "org.webjars" % "requirejs" % "2.1.14-1",
  "org.webjars.bower" % "react" % "15.1.0",
  "org.webjars.bower" % "es6-promise" % "3.2.2",
  "org.webjars.bower" % "fetch" % "1.0.0",
  "org.webjars.bower" % "codemirror" % "5.15.2",
  "org.webjars.bower" % "jshint" % "2.9.2",
  "org.webjars.bower" % "javascript-debounce" % "1.0.0",
  "org.webjars.bower" % "moment" % "2.14.1",
  "com.atlassian.commonmark" % "commonmark" % "0.6.0",
  "com.atlassian.commonmark" % "commonmark-ext-gfm-strikethrough" % "0.6.0",
  "com.atlassian.commonmark" % "commonmark-ext-autolink" % "0.6.0",
  "com.joestelmach" % "natty" % "0.11"
)

javaOptions in Test += "-Dconfig.file=conf/test.conf"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

RjsKeys.mainConfig := "build"
RjsKeys.mainModule := "build"
updateOptions := updateOptions.value.withCachedResolution(true)
