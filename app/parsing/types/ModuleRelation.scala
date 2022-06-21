package parsing.types

sealed trait ModuleRelation

object ModuleRelation {
  case class Parent(children: List[String]) extends ModuleRelation
  case class Child(parent: String) extends ModuleRelation
}
