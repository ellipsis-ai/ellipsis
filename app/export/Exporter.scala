package export

import java.io.{File, PrintWriter}

import scala.reflect.io.Path

trait Exporter {

  val fullPath: String

  protected def writeFileFor(filename: String, content: String): Unit = {
    val writer = new PrintWriter(new File(s"$fullPath/$filename"))
    writer.write(content)
    writer.close()
  }

  protected def writeFiles(): Unit

  def createDirectory(): Unit = {
    val path = Path(fullPath)
    path.deleteRecursively()
    path.createDirectory()
    writeFiles()
  }

}
