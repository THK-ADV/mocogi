package validator

sealed trait ValidModuleRelation

object ValidModuleRelation {
  case class Parent(children: List[Module]) extends ValidModuleRelation
  case class Child(parent: Module) extends ValidModuleRelation
}
