package parsing.compendium

import helper.FakeLocations
import service.LocationService

import scala.concurrent.Future

class FakeLocationService extends LocationService with FakeLocations {
  override def repo = ???
  override def parser = ???
  override def all() = Future.successful(fakeLocations)
}
