package parsing.metadata

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues

final class AttendanceRequirementParserSpec extends AnyWordSpec with EitherValues {

  import AttendanceRequirementParser.parser

  "An AttendanceRequirementParser" should {
    "parse an entry" in {
      val input = """attendance_requirement:
                    |  min: 5 von 7 Terminen (Beispiel)
                    |  reason: Hier Begründung
                    |  absence: Umgang mit Fehlzeiten eintragen""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(rest.isEmpty)
      assert(res.value.min == "5 von 7 Terminen (Beispiel)")
      assert(res.value.reason == "Hier Begründung")
      assert(res.value.absence == "Umgang mit Fehlzeiten eintragen")
    }
  }
}
