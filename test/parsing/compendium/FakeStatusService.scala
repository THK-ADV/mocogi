package parsing.compendium

import helper.FakeStatus
import service.StatusService

import scala.concurrent.Future

class FakeStatusService extends StatusService with FakeStatus {
  override def repo = ???
  override def parser = ???
  override def all() = Future.successful(fakeStatus)
}
