package controllers

import controllers.formats.ModuleRelationFormat
import models.Module
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import validator.ModuleRelation

import java.util.UUID

class ModuleRelationFormatSpec extends AnyWordSpec with ModuleRelationFormat {

  private lazy val m1 = Module(UUID.randomUUID, "m1", "m1")
  private lazy val m2 = Module(UUID.randomUUID, "m2", "m2")
  private lazy val m3 = Module(UUID.randomUUID, "m3", "m3")

  "A Module Relation Format Spec" should {
    "convert a parent object to json and parse it back to the original object" in {
      val parent: ModuleRelation =
        ModuleRelation.Parent(List(m1, m2, m3))
      val json = moduleRelationFormat.writes(parent)
      assert(
        json == Json.obj(
          "kind" -> "parent",
          "children" -> Json.toJson(List(m1, m2, m3))
        )
      )
      assert(moduleRelationFormat.reads(json).get == parent)
    }

    "convert a child object to json and parse it back to the original object" in {
      val child: ModuleRelation = ModuleRelation.Child(m1)
      val json = moduleRelationFormat.writes(child)
      assert(json == Json.obj("kind" -> "child", "parent" -> Json.toJson(m1)))
      assert(moduleRelationFormat.reads(json).get == child)
    }
  }
}
