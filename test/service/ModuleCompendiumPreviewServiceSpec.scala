package service

import org.scalatest.wordspec.AnyWordSpec

class ModuleCompendiumPreviewServiceSpec extends AnyWordSpec {

  import ModuleCompendiumPreviewService.containsPO

  "A ModuleCompendiumPreviewService" should {
    "succeed if input contains a given po in single entry" in {
      val input = """---v1s
                     |id: 0df7ec6b-747c-4b4c-b770-1d96a8c4d9f8
                     |title: Advanced Business Intelligence and Analytics
                     |po_mandatory:
                     |  - study_program: study_program.inf_dsi1
                     |participants:
                     |  min: 5
                     |  max: 25
                     |---""".stripMargin
      assert(containsPO(input, "inf_dsi1"))
    }

    "succeed if input contains a given po within multiple entries" in {
      val input = """---v1s
                    |id: 0e229c2a-a8de-4732-92cd-6cbd2f40a64d
                    |title: Programmieren
                    |po_mandatory:
                    |  - study_program: study_program.ing_een4
                    |    recommended_semester: 3
                    |  - study_program: study_program.ing_wiw4
                    |    recommended_semester: 3
                    |---""".stripMargin
      assert(containsPO(input, "ing_wiw4"))
    }

    "fail if input does not contain a given po in single entry" in {
      val input = """---v1s
                    |id: 0df7ec6b-747c-4b4c-b770-1d96a8c4d9f8
                    |title: Advanced Business Intelligence and Analytics
                    |po_mandatory:
                    |  - study_program: study_program.inf_dsi1
                    |participants:
                    |  min: 5
                    |  max: 25
                    |---""".stripMargin
      assert(!containsPO(input, "inf_abc"))
    }

    "fail if input does not contain a given po within multiple entries" in {
      val input = """---v1s
                    |id: 0e229c2a-a8de-4732-92cd-6cbd2f40a64d
                    |title: Programmieren
                    |po_mandatory:
                    |  - study_program: study_program.ing_een4
                    |    recommended_semester: 3
                    |  - study_program: study_program.ing_wiw4
                    |    recommended_semester: 3
                    |---""".stripMargin
      assert(!containsPO(input, "inf_abc"))
    }

    "fail if input does not contain a mandatory po at all" in {
      val input = """---v1s
                    |id: 0e229c2a-a8de-4732-92cd-6cbd2f40a64d
                    |title: Programmieren
                    |---""".stripMargin
      assert(!containsPO(input, "inf_abc"))
    }
  }
}
