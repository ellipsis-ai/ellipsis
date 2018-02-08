import com.typesafe.sbt.web._
import com.typesafe.sbt.web.pipeline.Pipeline
import sbt.Keys._
import sbt._

object Imports {
  // The task that will be part of sbt's asset pipeline
  val webpack = taskKey[Pipeline.Stage]("Webpack the JS files")

  // Namespace settings so we don't have to prefix them
  object WebpackKeys {
    // Customize concat
    val targetDir = settingKey[String]("The directory that should hold the files")
  }

}

/**
  * sbt-web plugin to concatenate files. Just like grunt-contrib-concat.
  */
object WebpackPlugin extends AutoPlugin {

  // Dependencies on other plugins, combine with '&&'
  override def requires = SbtWeb

  // NoTrigger = Must be enabled manually
  // AllRequirements = If required project is enabled, this will be as well
  override def trigger = AllRequirements

  // User of our plugin doesn't have to import concat task himself, autoImport puts it into scope
  val autoImport = Imports

  import autoImport._
  import com.typesafe.sbt.web.Import._, WebKeys._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    WebpackKeys.targetDir := "webpack",
    webpack := { mappings: Seq[PathMapping] =>
      // sourceManaged is crucial here: that ensures the assets are on the web server in production
      val targetDir = (sourceManaged in Assets).value / "webpack"
      val finalDir = targetDir / "javascripts"
      val sourceDir = (sourceDirectory in Assets).value / "frontend"
      Process("npm run build", sourceDir, ("WEBPACK_BUILD_PATH", finalDir.toPath.toString)).!
      val files = IO.listFiles(finalDir)
      val webpackMapping = files.toSeq pair relativeTo(targetDir)
      webpackMapping ++ mappings
    }
  )
}
