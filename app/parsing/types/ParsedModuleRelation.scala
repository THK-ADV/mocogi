package parsing.types

import java.util.UUID

sealed trait ParsedModuleRelation

object ParsedModuleRelation {
  case class Parent(children: List[UUID]) extends ParsedModuleRelation
  case class Child(parent: UUID) extends ParsedModuleRelation
}
