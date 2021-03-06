resolvers += Resolver.typesafeIvyRepo("releases")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.21")

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-web" % "1.4.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.1")
