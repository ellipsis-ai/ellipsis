package export

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.util.zip.{ZipEntry, ZipInputStream}

import scala.concurrent.Future

trait ZipImporter[T] {

  val zipFile: File

  protected def readDataFrom(zipInputStream: ZipInputStream): String = {
    val buffer = new Array[Byte](1024)
    val out = new ByteArrayOutputStream()
    var len: Int = zipInputStream.read(buffer)

    while (len > 0) {
      out.write(buffer, 0, len)
      len = zipInputStream.read(buffer)
    }

    out.toString
  }

  protected def importerFrom(strings: scala.collection.mutable.Map[String, String]): Importer[T]

  def run: Future[T] = {

    val zipInputStream: ZipInputStream = new ZipInputStream(new FileInputStream(zipFile))
    var entry: ZipEntry = zipInputStream.getNextEntry
    val strings = scala.collection.mutable.Map[String, String]()

    while (entry != null) {
      strings.put(entry.getName, readDataFrom(zipInputStream))
      entry = zipInputStream.getNextEntry
    }

    importerFrom(strings).run
  }

}
