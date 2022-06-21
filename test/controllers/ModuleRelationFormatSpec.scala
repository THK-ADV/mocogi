package controllers

import org.scalatest.wordspec.AnyWordSpec
import parsing.types.ModuleRelation
import play.api.libs.json.Json

class ModuleRelationFormatSpec extends AnyWordSpec {
  import ModuleCompendiumParsingController.moduleRelationFormat

  "A Module Relation Format Spec" should {
    "convert a parent object to json and parse it back to the original object" in {
      val parent: ModuleRelation =
        ModuleRelation.Parent(List("a", "b", "c"))
      val json = moduleRelationFormat.writes(parent)
      assert(
        json == Json.obj("type" -> "parent", "children" -> List("a", "b", "c"))
      )
      assert(moduleRelationFormat.reads(json).get == parent)
    }

    "convert a child object to json and parse it back to the original object" in {
      val child: ModuleRelation = ModuleRelation.Child("a")
      val json = moduleRelationFormat.writes(child)
      assert(json == Json.obj("type" -> "child", "parent" -> "a"))
      assert(moduleRelationFormat.reads(json).get == child)
    }
  }
}
