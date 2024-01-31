package parsing.core

import helper.FakeStudyPrograms
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import parsing.core.POFileParser.fileParser
import parsing.{ParserSpecHelper, withFile0}

import java.time.LocalDate

class POFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with OptionValues
    with FakeStudyPrograms {

  "A PO File Parser" should {
    "parse a single po" in {
      val input =
        """# test
          |ing_inf1:
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
      assert(po1.id == "ing_inf1")
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
      assert(po1.program == "inf_inf")
      assert(rest.isEmpty)
    }

    "parse mim2" in {
      val input =
        """inf_mim2:
          |  version: 2
          |  date: 05.04.2007
          |  date_from: 01.09.2001
          |  date_to: 29.02.2020
          |  modification_dates:
          |    - 16.06.2009
          |  program: program.inf_mim""".stripMargin
      val (res, rest) = fileParser.parse(input)
      val po1 = res.value.head
      assert(po1.id == "inf_mim2")
      assert(po1.version == 2)
      assert(po1.date == LocalDate.of(2007, 4, 5))
      assert(po1.dateFrom == LocalDate.of(2001, 9, 1))
      assert(po1.dateTo.value == LocalDate.of(2020, 2, 29))
      assert(
        po1.modificationDates == List(
          LocalDate.of(2009, 6, 16)
        )
      )
      assert(po1.program == "inf_mim")
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
          |# test
          |ing_gme4:
          |  version: 4
          |  date: 05.01.2021
          |  date_from: 01.03.2021
          |  program: program.inf_inf""".stripMargin
      val (res, rest) = fileParser.parse(input)
      val po1 = res.value.head
      assert(po1.id == "ing_inf1")
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
      assert(po1.program == "inf_inf")
      val po2 = res.value(1)
      assert(po2.id == "ing_gme4")
      assert(po2.version == 4)
      assert(po2.date == LocalDate.of(2021, 1, 5))
      assert(po2.dateFrom == LocalDate.of(2021, 3, 1))
      assert(po2.dateTo.isEmpty)
      assert(po2.modificationDates.isEmpty)
      assert(po2.program == "inf_inf")
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
