package parsing.core

import models.core.GlobalCriteria
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.core.GlobalCriteriaFileParser.fileParser
import parsing.{ParserSpecHelper, withFile0}

class GlobalCriteriaFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Global Criteria File Parser" should {
    "parse a single global criteria" in {
      val input =
        """employability:
          |  de_label: Employability
          |  de_desc: >
          |    In unseren Studiengängen qualifizieren wir unsere Studierenden für komplexe Tätigkeiten in einer sich wandelnden, 
          |    arbeitsteiligen, zunehmend digitalisierten und internationalen Berufswelt und befähigen sie zur verantwortlichen
          |    Mitgestaltung ihrer Arbeits- und Lebenswelt. Employability und Global Citizenship bedingen sich in unserem
          |    Verständnis gegenseitig. Daher beinhaltet Employability nicht nur eine Ausbildungsfunktion, sondern fordert immer
          |    auch die Bildungsfunktion im Medium der Wissenschaft.
          |  en_label: Employability""".stripMargin
      val (res, rest) = fileParser.parse(input)
      val GlobalCriteria(abbrev, deLabel, deDesc, enLabel, enDesc) =
        res.value.head
      assert(abbrev == "employability")
      assert(deLabel == "Employability")
      assert(deDesc.nonEmpty)
      assert(enLabel == "Employability")
      assert(enDesc.isEmpty)
      assert(rest.isEmpty)
    }

    "parse all in global_criteria.yaml" in {
      val (res1, rest1) =
        withFile0("test/parsing/res/global_criteria.yaml")(fileParser.parse)
      assert(res1.value.size == 10)
      assert(rest1.isEmpty)
    }
  }
}
