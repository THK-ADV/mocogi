package parsing.compendium

import helper.FakeModuleTypes
import service.ModuleTypeService

import scala.concurrent.Future

class FakeModuleTypeService extends ModuleTypeService with FakeModuleTypes {
  override def repo = ???
  override def parser = ???
  override def all() = Future.successful(fakeModuleTypes)
}
