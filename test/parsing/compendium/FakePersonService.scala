package parsing.compendium

import helper.FakePersons
import service.PersonService

import scala.concurrent.Future

class FakePersonService extends PersonService with FakePersons {
  override def parser = ???
  override def repo = ???
  override def all() = Future.successful(fakePersons)
}
