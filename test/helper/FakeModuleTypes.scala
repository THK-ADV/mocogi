package helper

import parsing.types.ModuleType

trait FakeModuleTypes {
  implicit def fakeModuleTypes: Seq[ModuleType] = Seq(
    ModuleType("mandatory", "Pflicht", "--"),
    ModuleType("wpf", "Wahlpflichtfach", "--")
  )
}
