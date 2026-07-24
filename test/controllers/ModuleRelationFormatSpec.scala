package controllers

import java.util.UUID

import cats.data.NonEmptyList
import models.ModuleCore
import models.ModuleRelation
import models.ModuleRelationProtocol
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class ModuleRelationFormatSpec extends AnyWordSpec {

  private lazy val m1 = ModuleCore(UUID.randomUUID, "m1", "m1")
  private lazy val m2 = ModuleCore(UUID.randomUUID, "m2", "m2")
  private lazy val m3 = ModuleCore(UUID.randomUUID, "m3", "m3")

  "A Module Relation Format Spec" should {
    "convert a parent object to json" in {
      val parent: ModuleRelation =
        ModuleRelation(NonEmptyList.of(m1, m2, m3))
      val json = ModuleRelation.writes.writes(parent)
      assert(
        json == Json.obj(
          "kind"     -> "parent",
          "children" -> Json.toJson(List(m1, m2, m3))
        )
      )
    }

    "reject the removed child representation" in {
      val json = Json.obj(
        "kind"   -> "child",
        "parent" -> UUID.randomUUID()
      )

      assert(json.validate[ModuleRelationProtocol].isError)
    }
  }
}
