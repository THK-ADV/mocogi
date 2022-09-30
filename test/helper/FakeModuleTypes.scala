package helper

import basedata.ModuleType

trait FakeModuleTypes {
  implicit def fakeModuleTypes: Seq[ModuleType] = Seq(
    ModuleType("module", "Modul", "--"),
    ModuleType("generic_module", "Generisches Modul", "--")
  )
}
