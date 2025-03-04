package controllers

import models.EmploymentType.Unknown
import models.core.Identity
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsArray, JsBoolean, JsString}

final class IdentityFormatSpec extends AnyWordSpec {
  "A Person Format Spec" should {
    "serialize a student" in {
      val student = Identity.Person(
        "1",
        "lastname",
        "firstname",
        "title",
        List("f1"),
        "p1",
        Some("id"),
        isActive = true,
        Unknown,
        None,
        None
      )
      val studentJson = Identity.writes.writes(student)
      assert((studentJson \ "id").get == JsString("1"))
      assert((studentJson \ "lastname").get == JsString("lastname"))
      assert((studentJson \ "firstname").get == JsString("firstname"))
      assert((studentJson \ "title").get == JsString("title"))
      assert(
        (studentJson \ "faculties").get == JsArray(student.faculties.map(JsString.apply))
      )
      assert((studentJson \ "abbreviation").get == JsString("p1"))
      assert((studentJson \ "campusId").get == JsString("id"))
      assert((studentJson \ "isActive").get == JsBoolean(true))
      assert((studentJson \ "employmentType").get == JsString("unknown"))
      assert((studentJson \ "imageUrl").isEmpty)
      assert((studentJson \ "websiteUrl").isEmpty)
      assert((studentJson \ "kind").get == JsString(Identity.PersonKind))
    }

    "serialize an unknown entry" in {
      val unknown = Identity.Unknown(
        "1",
        "unknown user"
      )
      val unknownJson = Identity.writes.writes(unknown)
      assert((unknownJson \ "id").get == JsString("1"))
      assert((unknownJson \ "label").get == JsString("unknown user"))
      assert((unknownJson \ "kind").get == JsString(Identity.UnknownKind))
    }

    "serialize a group" in {
      val group = Identity.Group(
        "1",
        "group user"
      )
      val groupJson = Identity.writes.writes(group)
      assert((groupJson \ "id").get == JsString("1"))
      assert((groupJson \ "label").get == JsString("group user"))
      assert((groupJson \ "kind").get == JsString(Identity.GroupKind))
    }
  }
}
