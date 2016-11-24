package export

import java.io.{File, PrintWriter}

import scala.reflect.io.Path

trait Exporter {

  val dirName: String

  protected def writeFileFor(filename: String, content: String): Unit = {
    val writer = new PrintWriter(new File(s"$dirName/$filename"))
    writer.write(content)
    writer.close()
  }

  protected def writeFiles(): Unit

  def createDirectory(): Unit = {
    val path = Path(dirName)
    path.deleteRecursively()
    path.createDirectory()
    writeFiles()
  }

}
