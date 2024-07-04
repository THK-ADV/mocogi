package parsing.types

import cats.data.NonEmptyList

import java.util.UUID

sealed trait ParsedModuleRelation

object ParsedModuleRelation {
  case class Parent(children: NonEmptyList[UUID]) extends ParsedModuleRelation
  case class Child(parent: UUID) extends ParsedModuleRelation
}
