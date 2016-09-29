package export

import java.io.{File, PrintWriter}

import scala.reflect.io.Path
import scala.sys.process.Process

trait Exporter {

  protected val exportId: String

  protected def dirName = s"/tmp/exports/${exportId}"
  protected def zipFileName = s"$dirName.zip"

  protected def writeFileFor(filename: String, content: String): Unit = {
    val writer = new PrintWriter(new File(s"$dirName/$filename"))
    writer.write(content)
    writer.close()
  }

  protected def writeFiles(): Unit

  protected def createZip(): Unit = {
    val path = Path(dirName)
    path.createDirectory()

    writeFiles()

    Process(Seq("bash","-c",s"cd $dirName && zip -r $zipFileName *")).!
  }

  def getZipFile: File = {
    createZip()
    new File(zipFileName)
  }

}
