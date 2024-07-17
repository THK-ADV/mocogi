package parsing.core

import models.core.ModuleGlobalCriteria
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.{ParserSpecHelper, withFile0}

class ModuleGlobalCriteriaFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  val parser = GlobalCriteriaFileParser.parser()

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
      val (res, rest) = parser.parse(input)
      val ModuleGlobalCriteria(abbrev, deLabel, deDesc, enLabel, enDesc) =
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
        withFile0("test/parsing/res/global_criteria.yaml")(parser.parse)
      val ids = List(
        "employability",
        "global_citizenship",
        "competence_orientation",
        "scientific",
        "diversity",
        "internationalization",
        "digitization",
        "democratization",
        "interdisciplinarity",
        "transfer"
      )
      res1.value.zip(ids).foreach { case (gc, id) =>
        assert(gc.id == id)
      }
      assert(rest1.isEmpty)

      val employability = res1.value.find(_.id == "employability").get
      assert(employability.deLabel == "Employability")
      assert(employability.enLabel == "Employability")
      assert(
        employability.deDesc.startsWith(
          "In unseren Studiengängen qualifizieren"
        )
      )
      assert(employability.deDesc.endsWith("im Medium der Wissenschaft."))
      assert(employability.enDesc.isEmpty)

      val globalCitizenship = res1.value.find(_.id == "global_citizenship").get
      assert(globalCitizenship.deLabel == "Global Citizenship")
      assert(globalCitizenship.enLabel == "Global Citizenship")
      assert(globalCitizenship.deDesc.startsWith("In unseren Studiengängen"))
      assert(globalCitizenship.deDesc.endsWith("Zusammenhängen zu entwickeln."))
      assert(globalCitizenship.enDesc.isEmpty)

      val competenceOrientation =
        res1.value.find(_.id == "competence_orientation").get
      assert(competenceOrientation.deLabel == "Kompetenzorientierung")
      assert(competenceOrientation.enLabel == "Competence orientation")
      assert(
        competenceOrientation.deDesc.startsWith("Kompetenzorientierung zielt")
      )
      assert(competenceOrientation.deDesc.endsWith(" Studiums aufweisen."))
      assert(competenceOrientation.enDesc.isEmpty)

      val scientific = res1.value.find(_.id == "scientific").get
      assert(scientific.deLabel == "Wissenschaftlichkeit")
      assert(scientific.enLabel == "Scientific")
      assert(scientific.deDesc.startsWith("In unseren Studiengängen gestalten"))
      assert(scientific.deDesc.endsWith("stellen diese zur Diskussion."))
      assert(scientific.enDesc.isEmpty)
    }
  }
}
