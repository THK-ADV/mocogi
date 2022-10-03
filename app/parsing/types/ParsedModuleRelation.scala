package parsing.types

sealed trait ParsedModuleRelation

object ParsedModuleRelation {
  case class Parent(children: List[String]) extends ParsedModuleRelation
  case class Child(parent: String) extends ParsedModuleRelation
}
