import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import utils.PageData

class PageDataSpec extends PlaySpec with MockitoSugar {
  "PageData.getPageData" should {
    "return page 1 if the count is equal to or less than the desired page size" in {
      PageData.getPageData(10, 2, 10) mustEqual(PageData(1, 10, 1))
      PageData.getPageData(30, 1, 50) mustEqual(PageData(1, 50, 1))
      PageData.getPageData(30, 5, 50) mustEqual(PageData(1, 50, 1))
      PageData.getPageData(50, 1, 50) mustEqual(PageData(1, 50, 1))
      PageData.getPageData(50, 5, 50) mustEqual(PageData(1, 50, 1))
    }

    "return the real last page if the desired page exceeds the number of pages" in {
      PageData.getPageData(25, 4, 10) mustEqual(PageData(3, 10, 3))
      PageData.getPageData(10, 11, 1) mustEqual(PageData(10, 1, 10))
    }

    "return the desired page and desired per-page size even if the page has fewer than requested" in {
      PageData.getPageData(18, 2, 10) mustEqual(PageData(2, 10, 2))
    }

    "return the desired page and per-page size if possible" in {
      PageData.getPageData(22, 2, 10) mustEqual(PageData(2, 10, 3))
      PageData.getPageData(99, 4, 20) mustEqual(PageData(4, 20, 5))
    }
  }
}
