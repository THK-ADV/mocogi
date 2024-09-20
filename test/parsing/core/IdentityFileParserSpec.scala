package parsing.core

import helper.FakeFaculties
import models.core.Identity
import models.core.Identity.Group
import models.core.Identity.Person
import models.core.Identity.Unknown
import models.core.PersonStatus
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.withFile0
import parsing.ParserSpecHelper

final class IdentityFileParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues with FakeFaculties {

  "A Person File Parser" should {
    "parse all people in person.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/person.yaml")(
          IdentityFileParser.fileParser(fakeFaculties.map(_.id)).parse
        )
      assert(res.value.size == 12)
      assert(rest.isEmpty)
      assert(res.value.head == Unknown("nn", "N.N."))
      assert(res.value(1) == Group("all", "alle aktiven Lehrenden der Hochschule"))
      assert(res.value(2) == Group("all-f10", "alle Lehrenden der F10"))
      assert(res.value(3) == Group("all-f10-prof", "alle Professor:innen der F10"))
      assert(res.value(4) == Group("all-inf", "alle Lehrenden der Lehreinheit Informatik"))
      assert(res.value(5) == Group("all-inf-prof", "alle Professor:innen der Lehreinheit Informatik"))
      assert(res.value(6) == Group("all-ing", "alle Lehrenden der Lehreinheit Ingenieurswesen"))
      assert(res.value(7) == Group("all-ing-prof", "alle Professor:innen der Lehreinheit Ingenieurswesen"))
      assert(res.value(8) == Person("abc", "foo", "bar", "", List("f10"), "abc", "", PersonStatus.Active))
      assert(res.value(9) == Person("def", "foo", "bar", "bar. baz.", List("f10"), "abc", "def", PersonStatus.Active))
      assert(res.value(10) == Person("ghi", "foo", "bar", "bar. baz.", List("f03"), "abc", "ghi", PersonStatus.Active))
      assert(
        res.value(11) == Person("jkl", "foo", "bar", "bar. baz.", List("f10", "f03"), "abc", "jkl", PersonStatus.Active)
      )
    }
  }
}
