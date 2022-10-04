package parsing.base

import basedata.StudyProgramPreview
import helper.FakeStudyProgramPreviews
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import parsing.base.POFileParser.fileParser
import parsing.{ParserSpecHelper, withFile0}

import java.time.LocalDate

class POFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with OptionValues
    with FakeStudyProgramPreviews {

  "A PO File Parser" should {
    "parse a single po" in {
      val input =
        """ing_inf1:
          |  version: 1
          |  date: 10.03.2009
          |  date_from: 01.09.2007
          |  date_to: 28.02.2021
          |  modification_dates:
          |    - 15.03.2012
          |    - 23.01.2015
          |    - 07.06.2016
          |  program: program.inf_inf""".stripMargin
      val (res, rest) = fileParser.parse(input)
      val po1 = res.value.head
      assert(po1.abbrev == "ing_inf1")
      assert(po1.version == 1)
      assert(po1.date == LocalDate.of(2009, 3, 10))
      assert(po1.dateFrom == LocalDate.of(2007, 9, 1))
      assert(po1.dateTo.value == LocalDate.of(2021, 2, 28))
      assert(
        po1.modificationDates == List(
          LocalDate.of(2012, 3, 15),
          LocalDate.of(2015, 1, 23),
          LocalDate.of(2016, 6, 7)
        )
      )
      assert(po1.program == StudyProgramPreview("inf_inf"))
      assert(rest.isEmpty)
    }

    "parse multiple pos" in {
      val input =
        """ing_inf1:
          |  version: 1
          |  date: 10.03.2009
          |  date_from: 01.09.2007
          |  date_to: 28.02.2021
          |  modification_dates:
          |    - 15.03.2012
          |    - 23.01.2015
          |    - 07.06.2016
          |  program: program.inf_inf
          |
          |ing_gme4:
          |  version: 4
          |  date: 05.01.2021
          |  date_from: 01.03.2021
          |  program: program.inf_inf""".stripMargin
      val (res, rest) = fileParser.parse(input)
      val po1 = res.value.head
      assert(po1.abbrev == "ing_inf1")
      assert(po1.version == 1)
      assert(po1.date == LocalDate.of(2009, 3, 10))
      assert(po1.dateFrom == LocalDate.of(2007, 9, 1))
      assert(po1.dateTo.value == LocalDate.of(2021, 2, 28))
      assert(
        po1.modificationDates == List(
          LocalDate.of(2012, 3, 15),
          LocalDate.of(2015, 1, 23),
          LocalDate.of(2016, 6, 7)
        )
      )
      assert(po1.program == StudyProgramPreview("inf_inf"))
      val po2 = res.value(1)
      assert(po2.abbrev == "ing_gme4")
      assert(po2.version == 4)
      assert(po2.date == LocalDate.of(2021, 1, 5))
      assert(po2.dateFrom == LocalDate.of(2021, 3, 1))
      assert(po2.dateTo.isEmpty)
      assert(po2.modificationDates.isEmpty)
      assert(po2.program == StudyProgramPreview("inf_inf"))
      assert(rest.isEmpty)
    }

    "parse all in po.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/po.yaml")(fileParser.parse)
      assert(res.value.size == 44)
      assert(rest.isEmpty)
    }
  }
}
