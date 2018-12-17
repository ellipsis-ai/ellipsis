import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import utils.{PageData, PageDataOutOfRangeException}

class PageDataSpec extends PlaySpec with MockitoSugar {
  "PageData.getPageData" should {
    "return page 1 if the count is equal to or less than the desired page size" in {
      PageData.pageDataFor(10, 2, 10) mustEqual(PageData(1, 10, 1))
      PageData.pageDataFor(30, 1, 50) mustEqual(PageData(1, 50, 1))
      PageData.pageDataFor(30, 5, 50) mustEqual(PageData(1, 50, 1))
      PageData.pageDataFor(50, 1, 50) mustEqual(PageData(1, 50, 1))
      PageData.pageDataFor(50, 5, 50) mustEqual(PageData(1, 50, 1))
    }

    "return the real last page if the desired page exceeds the number of pages" in {
      PageData.pageDataFor(25, 4, 10) mustEqual(PageData(3, 10, 3))
      PageData.pageDataFor(10, 11, 1) mustEqual(PageData(10, 1, 10))
    }

    "return the desired page and desired per-page size even if the page has fewer than requested" in {
      PageData.pageDataFor(18, 2, 10) mustEqual(PageData(2, 10, 2))
    }

    "return the desired page and per-page size if possible" in {
      PageData.pageDataFor(22, 2, 10) mustEqual(PageData(2, 10, 3))
      PageData.pageDataFor(99, 4, 20) mustEqual(PageData(4, 20, 5))
    }

    "throw an exception if passed zero or negative integers" in {
      a[PageDataOutOfRangeException] mustBe thrownBy {
        PageData.pageDataFor(-1, 1, 1)
      }
      a[PageDataOutOfRangeException] mustBe thrownBy {
        PageData.pageDataFor(0, 1, 1)
      }
      a[PageDataOutOfRangeException] mustBe thrownBy {
        PageData.pageDataFor(1, -1, 1)
      }
      a[PageDataOutOfRangeException] mustBe thrownBy {
        PageData.pageDataFor(1, 0, 1)
      }
      a[PageDataOutOfRangeException] mustBe thrownBy {
        PageData.pageDataFor(1, 1, 0)
      }
      a[PageDataOutOfRangeException] mustBe thrownBy {
        PageData.pageDataFor(1, 1, -1)
      }
    }
  }
}
