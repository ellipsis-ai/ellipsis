package utils

case class PageDataOutOfRangeException(propName: String) extends Exception {
  val message = s"$propName cannot be less than 1"
}

case class PageData(currentPage: Int, pageSize: Int, totalPages: Int)

object PageData {
  def pageDataFor(count: Int, desiredPage: Int, desiredPageSize: Int): PageData = {
    if (count < 1) {
      throw PageDataOutOfRangeException("count")
    }
    if (desiredPage < 1) {
      throw PageDataOutOfRangeException("desiredPage")
    }
    if (desiredPageSize < 1) {
      throw PageDataOutOfRangeException("desiredPageSize")
    }

    val numPages = math.ceil(count.toDouble / desiredPageSize.toDouble).toInt
    val currentPage = if (numPages < desiredPage) {
      numPages
    } else {
      desiredPage
    }

    PageData(currentPage, desiredPageSize, numPages)
  }
}

