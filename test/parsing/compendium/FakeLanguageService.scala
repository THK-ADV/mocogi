package parsing.compendium

import helper.FakeLanguages
import service.LanguageService

import scala.concurrent.Future

class FakeLanguageService extends LanguageService with FakeLanguages {
  override def parser = ???
  override def repo = ???
  override def all() = Future.successful(fakeLanguages)
}
