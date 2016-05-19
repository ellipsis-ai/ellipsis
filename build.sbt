name := """ellipsis"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)
pipelineStages := Seq(rjs, digest)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  evolutions,
  jdbc,
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
  "org.webjars" % "react" % "15.0.1"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"
