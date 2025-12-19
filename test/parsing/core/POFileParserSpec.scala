package parsing.core

import java.time.LocalDate

import helper.FakeStudyPrograms
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import org.scalatest.OptionValues
import parsing.core.POFileParser.fileParser
import parsing.withFile0
import parsing.ParserSpecHelper

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
      val po1         = res.value.head
      assert(po1.id == "ing_inf1")
      assert(po1.version == 1)
      assert(po1.dateFrom == LocalDate.of(2007, 9, 1))
      assert(po1.dateTo.value == LocalDate.of(2021, 2, 28))
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
      val po1         = res.value.head
      assert(po1.id == "inf_mim2")
      assert(po1.version == 2)
      assert(po1.dateFrom == LocalDate.of(2001, 9, 1))
      assert(po1.dateTo.value == LocalDate.of(2020, 2, 29))
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
      val po1         = res.value.head
      assert(po1.id == "ing_inf1")
      assert(po1.version == 1)
      assert(po1.dateFrom == LocalDate.of(2007, 9, 1))
      assert(po1.dateTo.value == LocalDate.of(2021, 2, 28))
      assert(po1.program == "inf_inf")
      val po2 = res.value(1)
      assert(po2.id == "ing_gme4")
      assert(po2.version == 4)
      assert(po2.dateFrom == LocalDate.of(2021, 3, 1))
      assert(po2.dateTo.isEmpty)
      assert(po2.program == "inf_inf")
      assert(rest.isEmpty)
    }

    "parse all in po.yaml" in {
      val (res, rest) = withFile0("test/parsing/res/po.yaml")(fileParser.parse)
      val ids         = List(
        "ing_gme1",
        "ing_gme3",
        "ing_gme4",
        "ing_ait1",
        "ing_ait2",
        "ing_ait3",
        "inf_coco1",
        "inf_dsi1",
        "ing_een1",
        "ing_een2",
        "ing_een3",
        "ing_een4",
        "inf_inf1",
        "inf_inf1_flex",
        "inf_inf2",
        "inf_itm1",
        "inf_itm2",
        "inf_mi2",
        "inf_mi3",
        "inf_mi4",
        "inf_mi5",
        "inf_mim2",
        "inf_mim3",
        "inf_mim4",
        "inf_mim5",
        "ing_pdpd1",
        "ing_pdpd2",
        "ing_pdpd3",
        "ing_pdpd4",
        "ing_pdpd5",
        "inf_wsc1",
        "inf_wi1",
        "inf_wi2",
        "inf_wi3",
        "inf_wi4",
        "inf_wi5",
        "inf_wiv1",
        "inf_wiv2",
        "inf_wivm1",
        "inf_wivm2",
        "ing_wiw1",
        "ing_wiw2",
        "ing_wiw3",
        "ing_wiw4",
        "ing_wiwm1",
        "ing_wiwm2"
      )
      res.value.zip(ids).foreach {
        case (po, id) =>
          assert(po.id == id)
          assert(po.program == "inf_inf")
      }

      val ait2 = res.value.find(_.id == "ing_ait2").value
      assert(ait2.version == 2)
      assert(ait2.dateFrom == LocalDate.of(2013, 9, 1))
      assert(ait2.dateTo.value == LocalDate.of(2023, 8, 31))

      val ait3 = res.value.find(_.id == "ing_ait3").value
      assert(ait3.version == 3)
      assert(ait3.dateFrom == LocalDate.of(2020, 9, 1))
      assert(ait3.dateTo.isEmpty)

      assert(rest.isEmpty)
    }
  }
}
