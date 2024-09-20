package git

import models.core.ModuleLocation
import monocle.macros.GenLens
import org.scalatest.wordspec.AnyWordSpec

final class CoreDataPublisherSpec extends AnyWordSpec {
  import git.publisher.CoreDataPublisher._

  "A Core Data Publisher" should {
    "split between create, update and delete" in {
      var existingValues: List[String] = Nil
      var newValues: List[String]      = List("A", "B")

      assert(toCreate(existingValues, newValues) == List("A", "B"))
      assert(toDelete(existingValues, newValues) == Nil)
      assert(toUpdate(existingValues, newValues) == Nil)

      existingValues = List("A", "B")
      newValues = List("A", "B", "C")

      assert(toCreate(existingValues, newValues) == List("C"))
      assert(toDelete(existingValues, newValues) == Nil)
      assert(toUpdate(existingValues, newValues) == List("A", "B"))

      existingValues = List("A", "B")
      newValues = List("A", "B")

      assert(toCreate(existingValues, newValues) == Nil)
      assert(toDelete(existingValues, newValues) == Nil)
      assert(toUpdate(existingValues, newValues) == List("A", "B"))

      existingValues = List("A", "B")
      newValues = List("A", "C")

      assert(toCreate(existingValues, newValues) == List("C"))
      assert(toDelete(existingValues, newValues) == List("B"))
      assert(toUpdate(existingValues, newValues) == List("A"))

      existingValues = List("A", "B")
      newValues = List("A")

      assert(toCreate(existingValues, newValues) == Nil)
      assert(toDelete(existingValues, newValues) == List("B"))
      assert(toUpdate(existingValues, newValues) == List("A"))

      existingValues = List("A", "B")
      newValues = Nil

      assert(toCreate(existingValues, newValues) == Nil)
      assert(toDelete(existingValues, newValues) == List("A", "B"))
      assert(toUpdate(existingValues, newValues) == Nil)

      existingValues = List("A", "B")
      newValues = List("C")

      assert(toCreate(existingValues, newValues) == List("C"))
      assert(toDelete(existingValues, newValues) == List("A", "B"))
      assert(toUpdate(existingValues, newValues) == Nil)
    }

    "split locations" in {
      var locations = List(
        ModuleLocation("A", "", ""),
        ModuleLocation("B", "", ""),
        ModuleLocation("C", "", "")
      )
      var existing = Seq("A", "B")

      var (toCreate, toUpdate, toDelete) =
        split(existing, locations, GenLens[ModuleLocation](_.id))
      assert(toCreate.size == 1)
      assert(toCreate.head == ModuleLocation("C", "", ""))
      assert(toUpdate.size == 2)
      assert(
        toUpdate == List(
          ModuleLocation("A", "", ""),
          ModuleLocation("B", "", "")
        )
      )
      assert(toDelete.isEmpty)

      locations = List(
        ModuleLocation("A", "", ""),
        ModuleLocation("C", "", "")
      )
      existing = Seq("A", "B")

      val res = split(existing, locations, GenLens[ModuleLocation](_.id))
      toCreate = res._1
      toUpdate = res._2
      toDelete = res._3

      assert(toCreate.size == 1)
      assert(toCreate.head == ModuleLocation("C", "", ""))
      assert(toUpdate.size == 1)
      assert(toUpdate.head == ModuleLocation("A", "", ""))
      assert(toDelete.size == 1)
      assert(toDelete.head == "B")
    }
  }
}
