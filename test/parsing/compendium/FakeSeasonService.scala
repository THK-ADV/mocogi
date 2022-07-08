package parsing.compendium

import service.SeasonService

import scala.concurrent.Future

class FakeSeasonService extends SeasonService with FakeSeasons {
  override def repo = ???
  override def parser = ???
  override def all() = Future.successful(fakeSeasons)
}
