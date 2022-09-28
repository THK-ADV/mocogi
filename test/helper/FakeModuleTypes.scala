package helper

import parsing.types.ModuleType

trait FakeModuleTypes {
  implicit def fakeModuleTypes: Seq[ModuleType] = Seq(
    ModuleType("module", "Modul", "--"),
    ModuleType("generic_module", "Generisches Modul", "--")
  )
}
