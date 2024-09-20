package parsing.types

import java.util.UUID

import cats.data.NonEmptyList

sealed trait ParsedModuleRelation

object ParsedModuleRelation {
  case class Parent(children: NonEmptyList[UUID]) extends ParsedModuleRelation
  case class Child(parent: UUID)                  extends ParsedModuleRelation
}
