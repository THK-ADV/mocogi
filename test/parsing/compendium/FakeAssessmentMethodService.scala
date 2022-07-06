package parsing.compendium

import helper.FakeAssessmentMethod
import service.AssessmentMethodService

import scala.concurrent.Future

class FakeAssessmentMethodService
    extends AssessmentMethodService
    with FakeAssessmentMethod {
  override def repo = ???
  override def parser = ???
  override def all() = Future.successful(fakeAssessmentMethod)
}
