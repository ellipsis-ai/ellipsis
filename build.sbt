import WebJs._
import RjsKeys._

name := """ellipsis"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)
pipelineStages := Seq(rjs, digest, gzip)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  evolutions,
  jdbc,
  filters,
  "com.zaxxer" % "HikariCP" % "2.4.1",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test,
  "com.github.gilbertw1" %% "slack-scala-client" % "0.1.4",
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
  "org.webjars.bower" % "jshint" % "2.8.0",
  "com.atlassian.commonmark" % "commonmark" % "0.3.0",
  "com.atlassian.commonmark" % "commonmark-ext-gfm-strikethrough" % "0.3.0",
  "com.atlassian.commonmark" % "commonmark-ext-autolink" % "0.3.0"
)

javaOptions in Test += "-Dconfig.file=conf/test.conf"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

RjsKeys.mainConfig := "build"
