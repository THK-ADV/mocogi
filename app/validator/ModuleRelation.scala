package validator

sealed trait ModuleRelation

object ModuleRelation {
  case class Parent(children: List[Module]) extends ModuleRelation
  case class Child(parent: Module) extends ModuleRelation
}
