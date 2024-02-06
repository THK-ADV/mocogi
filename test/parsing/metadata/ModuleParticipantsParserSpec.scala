package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.ModuleParticipantsParser._
import parsing.types.ModuleParticipants

final class ModuleParticipantsParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Participants Parser should" should {
    "parse participants" in {
      val input1 =
        """participants:
          |  min: 4
          |  max: 20""".stripMargin
      val (res1, rest1) = participantsParser.parse(input1)
      assert(rest1.isEmpty)
      assert(res1.value == ModuleParticipants(4, 20))
    }

    "fail parsing participants if no value is provided" in {
      assertError(
        participantsParser,
        """participants:
          |  min:
          |  max: 20""".stripMargin,
        "an integer",
        Some("max: 20")
      )
      assertError(
        participantsParser,
        """participants:
          |  min: 0
          |  max: """.stripMargin,
        "an integer",
        Some("")
      )
    }

    "fail parsing participants if min / max keys are missing or in different order" in {
      assertError(
        participantsParser,
        """participants:
          |  max: 20""".stripMargin,
        "min:",
        Some("max: 20")
      )
      assertError(
        participantsParser,
        """participants:
          |  min: 20""".stripMargin,
        "max:",
        Some("")
      )
      assertError(
        participantsParser,
        """participants:
          |  max: 30
          |  min: 20""".stripMargin,
        "min:",
        Some("max: 30\n  min: 20")
      )
    }

    "fail parsing participants if min is higher than max" in {
      assertError(
        participantsParser,
        """participants:
          |  min: 30
          |  max: 20""".stripMargin,
        "min 30 should be lower than max 20",
        Some("")
      )
    }
  }
}
