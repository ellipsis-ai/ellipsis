package utils

case class PageData(currentPage: Int, pageSize: Int, totalPages: Int)

object PageData {
  def getPageData(count: Int, desiredPage: Int, desiredPageSize: Int): PageData = {
    val numPages = math.ceil(count.toDouble / desiredPageSize.toDouble).toInt
    val currentPage = if (numPages < desiredPage) {
      numPages
    } else {
      desiredPage
    }

    PageData(currentPage, desiredPageSize, numPages)
  }
}

