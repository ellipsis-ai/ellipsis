import java.time.OffsetDateTime

import models.IDs
import models.behaviors.behaviorgroupversion.BehaviorGroupVersion
import models.behaviors.library.LibraryVersion
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import utils.RequiredModulesInCode

class RequiredModulesInCodeSpec extends PlaySpec with MockitoSugar {

  def checkModulesFor(
                     code: String,
                     expectedResult: Array[String],
                     libraries: Seq[LibraryVersion] = Seq()
                     ): Unit = {
    val modules = RequiredModulesInCode.requiredModulesIn(code, libraries, includeLibraryRequires = true)
    modules mustBe expectedResult
  }

  "RequiredModulesInCode" should {

    "work for single quotes" in {
      val code =
        """
          |const required = require('foo');
        """.stripMargin
      checkModulesFor(code, Array("foo"))
    }

    "work for double quotes" in {
      val code =
        """
          |const required = require("foo");
        """.stripMargin
      checkModulesFor(code, Array("foo"))
    }

    "ignore whitespace" in {
      val code =
        """
          |const required = require(" foo  ");
        """.stripMargin
      checkModulesFor(code, Array("foo"))
    }

    "ignore library names" in {
      val libName = "some_lib"
      val libraries = Seq(LibraryVersion(IDs.next, IDs.next, None, libName, None, "", mock[BehaviorGroupVersion], OffsetDateTime.now))
      val code =
        s"""
          |const required = require("$libName");
        """.stripMargin
      checkModulesFor(code, Array(), libraries)
    }

    "ignore library names in current dir" in {
      val libName = "some_lib"
      val libraries = Seq(LibraryVersion(IDs.next, IDs.next, None, libName, None, "", mock[BehaviorGroupVersion], OffsetDateTime.now))
      val code =
        s"""
           |const required = require("./$libName");
        """.stripMargin
      checkModulesFor(code, Array(), libraries)
    }

    "include requires in library code" in {
      val libName = "some_lib"
      val moduleName = "ellipsis-api"
      val libCode =
        s"""
          |const required = require('$moduleName');
        """.stripMargin
      val libraries = Seq(LibraryVersion(IDs.next, IDs.next, None, libName, None, libCode, mock[BehaviorGroupVersion], OffsetDateTime.now))
      val code =
        s"""
           |const required = require("./$libName");
        """.stripMargin
      checkModulesFor(code, Array(moduleName), libraries)
    }

  }

}
