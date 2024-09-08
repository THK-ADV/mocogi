package parsing.metadata

import helper.FakeIdentities
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}

final class ExaminerParserSpec
    extends AnyWordSpec
    with EitherValues
    with OptionValues
    with FakeIdentities {

  import ExaminerParser.{parser, raw}

  "A Examiner Parser" should {
    "parse examiner" in {
      val input = "first_examiner: person.ald\nsecond_examiner: person.abe\n"
      val (res, rest) = parser.parse(input)
      assert(res.value.first == fakeIdentities.find(_.id == "ald").value)
      assert(res.value.second == fakeIdentities.find(_.id == "abe").value)
      assert(rest.isEmpty)
    }

    "parse examiner even if empty" in {
      val input = "foo: bar"
      val (res, rest) = parser.parse(input)
      assert(rest == input)
      assert(res.value.first == unknown)
      assert(res.value.second == unknown)
    }

    "parse examiner raw" in {
      val input = "first_examiner: person.ald\nsecond_examiner: person.abe\n"
      val (res, rest) = raw.parse(input)
      assert(rest.isEmpty)
      assert(res.value.first == "ald")
      assert(res.value.second == "abe")
    }

    "parse examiner raw even if empty" in {
      val input = "foo: bar"
      val (res, rest) = raw.parse(input)
      assert(rest == input)
      assert(res.value.first == unknown.id)
      assert(res.value.second == unknown.id)
    }
  }
}
