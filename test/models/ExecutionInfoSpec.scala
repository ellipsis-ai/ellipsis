package models

import models.behaviors._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import utils.UploadFileSpec

class ExecutionInfoSpec extends PlaySpec {
  "withUserFilesFrom" should {
    "return files specified in valid JSON" in {
      val picUrl = "https://vignette.wikia.nocookie.net/2001/images/a/a5/EE46A295-E2F9-4619-ABF4-E5BE0D31C0E3.jpeg/revision/latest/scale-to-width-down/620?cb=20190114052106"
      val info = ExecutionInfo.empty.withUserFilesFrom(Json.parse(
        s"""{
          |  "files": [{
          |    "content": "A file",
          |    "filetype": "text",
          |    "filename": "file.txt"
          |  }, {
          |    "content": "{}",
          |    "filetype": "json",
          |    "filename": "file.json"
          |  }, {
          |    "url": "${picUrl}",
          |    "filetype": "jpg",
          |    "filename": "hal.jpg"
          |  }]
          |}
        """.stripMargin
      ))
      info.userFiles mustEqual Seq(
        UploadFileSpec(None, Some("A file"), Some("text"), Some("file.txt")),
        UploadFileSpec(None, Some("{}"), Some("json"), Some("file.json")),
        UploadFileSpec(Some(picUrl), None, Some("jpg"), Some("hal.jpg"))
      )
      info.errors mustBe empty
    }

    "return errors for invalid JSON" in {
      val info = ExecutionInfo.empty.withUserFilesFrom(Json.parse(
        """{
          |  "files": {
          |    "content": "A file",
          |    "filetype": "text",
          |    "filename": "file.txt"
          |  }
          |}
        """.stripMargin
      ))
      info.userFiles mustBe empty
      info.errors.head mustBe a [UserFilesError]
    }

    "return no files or errors when no files property is in the JSON" in {
      val info = ExecutionInfo.empty.withUserFilesFrom(Json.parse(
        """{
          |  "choices": []
          |}
        """.stripMargin
      ))
      info.userFiles mustBe empty
      info.errors mustBe empty
    }
  }

  "withChoicesFrom" should {
    "return choices from valid JSON" in {
      val info = ExecutionInfo.empty.withChoicesFrom(Json.parse(
        """{
          |  "choices": [{
          |    "actionName": "Foo",
          |    "label": "A label"
          |  }, {
          |    "actionName": "Bar",
          |    "label": "Another label",
          |    "args": [{
          |      "name": "foo",
          |      "value": "bar"
          |    }],
          |    "allowOthers": true,
          |    "allowMultipleSelections": true,
          |    "quiet": true
          |  }]
          |}
        """.stripMargin
      ))
      info.choices mustEqual Seq(
        SkillCodeActionChoice("A label", "Foo", None, None, None, None, None, None),
        SkillCodeActionChoice("Another label", "Bar", Some(Seq(ActionArg("foo", "bar"))), Some(true), Some(true), Some(true), None, None)
      )
      info.errors mustBe empty
    }

    "return errors for invalid JSON" in {
      val info = ExecutionInfo.empty.withChoicesFrom(Json.parse(
        """{
          |  "choices": [{
          |    "actionName": "Foo",
          |    "label": "A label"
          |  }, {
          |    "actionName": "Bar",
          |    "label": "Another label",
          |    "args": [{
          |      "name": "foo",
          |      "value": null
          |    }],
          |    "allowOthers": true,
          |    "allowMultipleSelections": true,
          |    "quiet": true
          |  }]
          |}
        """.stripMargin
      ))
      info.choices mustBe empty
      info.errors.head mustBe a [ChoicesError]
    }

    "return no files or errors when no choices property is in the JSON" in {
      val info = ExecutionInfo.empty.withChoicesFrom(Json.parse(
        """{
          |  "files": []
          |}
        """.stripMargin
      ))
      info.choices mustBe empty
      info.errors mustBe empty
    }
  }

  "withNextActionFrom" should {
    "return a next action from valid JSON" in {
      val info = ExecutionInfo.empty.withNextActionFrom(Json.parse(
        """{
          |  "next": {
          |    "actionName": "Foo",
          |    "args": [{
          |      "name": "foo",
          |      "value": "bar"
          |    }]
          |  }
          |}
        """.stripMargin
      ))
      info.maybeNextAction mustEqual Some(NextAction("Foo", Some(Seq(ActionArg("foo", "bar")))))
      info.errors mustBe empty
    }

    "return errors from invalid JSON" in {
      val info = ExecutionInfo.empty.withNextActionFrom(Json.parse(
        """{
          |  "next": {
          |    "actionName": "Foo",
          |    "args": [{
          |      "name": "foo",
          |      "value": true
          |    }]
          |  }
          |}
        """.stripMargin
      ))
      info.maybeNextAction mustBe None
      info.errors.head mustBe a [NextActionError]
    }

    "return neither a next action nor errors when no next property is in the JSON" in {
      val info = ExecutionInfo.empty.withNextActionFrom(Json.parse(
        """{
          |  "files": []
          |}
        """.stripMargin
      ))
      info.maybeNextAction mustBe None
      info.errors mustBe empty
    }
  }
}
