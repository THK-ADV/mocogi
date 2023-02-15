package controllers

import controllers.formats.PersonFormat
import models.core.{Faculty, Person, PersonStatus}
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsString, Writes}

final class PersonFormatSpec extends AnyWordSpec with PersonFormat {
  "A Person Format Spec" should {
    "serialize back and forth student" in {
      val student = Person.Single(
        "1",
        "lastname",
        "firstname",
        "title",
        List(Faculty("f1", "de", "en")),
        "p1",
        PersonStatus.Active
      )
      val studentJson = personFormat.writes(student)
      assert((studentJson \ "id").get == JsString("1"))
      assert((studentJson \ "lastname").get == JsString("lastname"))
      assert((studentJson \ "firstname").get == JsString("firstname"))
      assert((studentJson \ "title").get == JsString("title"))
      assert(
        (studentJson \ "faculties").get == Writes
          .list[Faculty]
          .writes(student.faculties)
      )
      assert((studentJson \ "abbreviation").get == JsString("p1"))
      assert((studentJson \ "status").get == JsString("active"))
      assert((studentJson \ "kind").get == JsString(Person.SingleKind))
    }

    "serialize back and forth unknown" in {
      val unknown = Person.Unknown(
        "1",
        "unknown user"
      )
      val unknownJson = personFormat.writes(unknown)
      assert((unknownJson \ "id").get == JsString("1"))
      assert((unknownJson \ "title").get == JsString("unknown user"))
      assert((unknownJson \ "kind").get == JsString(Person.UnknownKind))
    }

    "serialize back and forth group" in {
      val group = Person.Group(
        "1",
        "group user"
      )
      val groupJson = personFormat.writes(group)
      assert((groupJson \ "id").get == JsString("1"))
      assert((groupJson \ "title").get == JsString("group user"))
      assert((groupJson \ "kind").get == JsString(Person.GroupKind))
    }
  }
}
