package parsing.base

import basedata._
import helper._
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.base.StudyProgramFileParser._
import parsing.{ParserSpecHelper, withFile0}

import java.time.LocalDate

final class StudyProgramFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeGrades
    with FakePersons
    with FakeStudyFormType
    with FakeLanguages
    with FakeSeasons
    with FakeLocations {

  "A StudyProgram File Parser" should {
    "parse labels" in {
      val input =
        """de_label: Digital Sciences
          |en_label: Digital Sciences""".stripMargin
      val (res, rest) = labelParser.parse(input)
      assert(res.value == ("Digital Sciences", "Digital Sciences"))
      assert(rest.isEmpty)
    }

    "parse abbreviation" in {
      val input1 =
        """abbreviation:
          |  internal: a""".stripMargin
      val (res1, rest1) = abbreviationParser.parse(input1)
      assert(res1.value == ("a", ""))
      assert(rest1.isEmpty)

      val input2 =
        """abbreviation:
          |  external: b""".stripMargin
      val (res2, rest2) = abbreviationParser.parse(input2)
      assert(res2.value == ("", "b"))
      assert(rest2.isEmpty)

      val input3 = "abbreviation:"
      val (res3, rest3) = abbreviationParser.parse(input3)
      assert(res3.value == ("", ""))
      assert(rest3.isEmpty)
    }

    "parse urls" in {
      val input1 =
        """de_url: www.foo.bar
          |en_url:""".stripMargin
      val (res1, rest1) = urlParser.parse(input1)
      assert(res1.value == ("www.foo.bar", ""))
      assert(rest1.isEmpty)

      val input2 =
        """de_url: www.foo.bar
          |en_url: www.foo.bar""".stripMargin
      val (res2, rest2) = urlParser.parse(input2)
      assert(res2.value == ("www.foo.bar", "www.foo.bar"))
      assert(rest2.isEmpty)
    }

    "parse grade" in {
      val input = "grade: grade.msc"
      val (res, rest) = gradeParser.parse(input)
      assert(res.value == msc)
      assert(rest.isEmpty)
    }

    "parse program director" in {
      val input = "program_director: person.ald"
      val (res, rest) = programDirectorParser.parse(input)
      assert(
        res.value == Person.Single(
          "ald",
          "Dobrynin",
          "Alexander",
          "M.Sc.",
          List(f10),
          "ad",
          PersonStatus.Active
        )
      )
      assert(rest.isEmpty)
    }

    "parse accreditation until date" in {
      val input = "accreditation_until: 30.09.2027"
      val (res, rest) = accreditationUntilParser.parse(input)
      assert(res.value == LocalDate.of(2027, 9, 30))
      assert(rest.isEmpty)
    }

    "parse study form scope" in {
      val input1 =
        """- program_duration: 4
          |  total_ECTS: 120""".stripMargin
      val (res1, rest1) = studyFormScopeParser.parse(input1)
      assert(res1.value == StudyFormScope(4, 120, "", ""))
      assert(rest1.isEmpty)

      val input2 =
        """- program_duration: 4
          |  total_ECTS: 120
          |  de_reason: a""".stripMargin
      val (res2, rest2) = studyFormScopeParser.parse(input2)
      assert(res2.value == StudyFormScope(4, 120, "a", ""))
      assert(rest2.isEmpty)

      val input3 =
        """- program_duration: 4
          |  total_ECTS: 120
          |  en_reason: a""".stripMargin
      val (res3, rest3) = studyFormScopeParser.parse(input3)
      assert(res3.value == StudyFormScope(4, 120, "", "a"))
      assert(rest3.isEmpty)

      val input4 =
        """- program_duration: 4
          |  total_ECTS: 120
          |  de_reason: a
          |  en_reason: b""".stripMargin
      val (res4, rest4) = studyFormScopeParser.parse(input4)
      assert(res4.value == StudyFormScope(4, 120, "a", "b"))
      assert(rest4.isEmpty)
    }

    "parse study form entry" in {
      val input1 =
        """- type: study_form.full
          |  workload_per_ects: 30
          |  scope:
          |    - program_duration: 4
          |      total_ECTS: 120""".stripMargin
      val (res1, rest1) = studyFormEntryParser.parse(input1)
      assert(
        res1.value == StudyForm(
          StudyFormType("full", "", ""),
          30,
          List(StudyFormScope(4, 120, "", ""))
        )
      )
      assert(rest1.isEmpty)

      val input2 =
        """- type: study_form.part
          |  workload_per_ects: 30
          |  scope:
          |  - program_duration: 4
          |    total_ECTS: 120
          |  - program_duration: 5
          |    total_ECTS: 150
          |    de_reason: a
          |    en_reason: b""".stripMargin
      val (res2, rest2) = studyFormEntryParser.parse(input2)
      assert(
        res2.value == StudyForm(
          StudyFormType("part", "", ""),
          30,
          List(
            StudyFormScope(4, 120, "", ""),
            StudyFormScope(5, 150, "a", "b")
          )
        )
      )
      assert(rest2.isEmpty)
    }

    "parse study form" in {
      val input2 =
        """study_form:
          |  - type: study_form.full
          |    workload_per_ects: 30
          |    scope:
          |      - program_duration: 4
          |        total_ECTS: 120
          |  - type: study_form.part
          |      workload_per_ects: 30
          |      scope:
          |      - program_duration: 4
          |        total_ECTS: 120
          |      - program_duration: 5
          |        total_ECTS: 150
          |        de_reason: a
          |        en_reason: b""".stripMargin
      val (res2, rest2) = studyFormParser.parse(input2)
      assert(
        res2.value == List(
          StudyForm(
            StudyFormType("full", "", ""),
            30,
            List(
              StudyFormScope(4, 120, "", "")
            )
          ),
          StudyForm(
            StudyFormType("part", "", ""),
            30,
            List(
              StudyFormScope(4, 120, "", ""),
              StudyFormScope(5, 150, "a", "b")
            )
          )
        )
      )
      assert(rest2.isEmpty)
    }

    "parse language of instructions" in {
      val input1 = "language_of_instruction: lang.de"
      val (res1, rest1) = languageParser.parse(input1)
      assert(res1.value == List(Language("de", "Deutsch", "--")))
      assert(rest1.isEmpty)

      val input2 =
        """language_of_instruction:
          |  - lang.de
          |  - lang.en""".stripMargin
      val (res2, rest2) = languageParser.parse(input2)
      assert(
        res2.value == List(
          Language("de", "Deutsch", "--"),
          Language("en", "Englisch", "--")
        )
      )
      assert(rest2.isEmpty)
    }

    "parse beginning of program" in {
      val input1 = "beginning_of_program: season.ws"
      val (res1, rest1) = seasonParser.parse(input1)
      assert(res1.value == List(Season("ws", "Wintersemester", "--")))
      assert(rest1.isEmpty)

      val input2 =
        """beginning_of_program:
          |  - season.ws
          |  - season.ss""".stripMargin
      val (res2, rest2) = seasonParser.parse(input2)
      assert(
        res2.value == List(
          Season("ws", "Wintersemester", "--"),
          Season("ss", "Sommersemester", "--")
        )
      )
      assert(rest2.isEmpty)
    }

    "parse campus" in {
      val input1 = "campus: location.gm"
      val (res1, rest1) = campusParser.parse(input1)
      assert(res1.value == List(Location("gm", "Gummersbach", "--")))
      assert(rest1.isEmpty)

      val input2 =
        """campus:
          |  - location.gm
          |  - location.dz""".stripMargin
      val (res2, rest2) = campusParser.parse(input2)
      assert(
        res2.value == List(
          Location("gm", "Gummersbach", "--"),
          Location("dz", "Deutz", "--")
        )
      )
      assert(rest2.isEmpty)
    }

    "parse restricted admission" in {
      val input1 =
        """restricted_admission:
          |  value: true""".stripMargin
      val (res1, rest1) = restrictedAdmissionParser().parse(input1)
      assert(res1.value == RestrictedAdmission(value = true, "", ""))
      assert(rest1.isEmpty)

      val input2 =
        """restricted_admission:
          |  value: false""".stripMargin
      val (res2, rest2) = restrictedAdmissionParser().parse(input2)
      assert(res2.value == RestrictedAdmission(value = false, "", ""))
      assert(rest2.isEmpty)

      val input3 =
        """restricted_admission:
          |  value: true
          |  de_reason: a
          |  en_reason: b""".stripMargin
      val (res3, rest3) = restrictedAdmissionParser().parse(input3)
      assert(res3.value == RestrictedAdmission(value = true, "a", "b"))
      assert(rest3.isEmpty)

      val input4 =
        """restricted_admission:
          |  value: true
          |  de_reason: a""".stripMargin
      val (res4, rest4) = restrictedAdmissionParser().parse(input4)
      assert(res4.value == RestrictedAdmission(value = true, "a", ""))
      assert(rest4.isEmpty)
    }

    "parse study program" in {
      val (res, rest) = withFile0("test/parsing/res/program1.yaml")(
        fileParser.parse
      )
      val sp1 = res.value.head
      val esp1 = StudyProgram(
        "inf_dsi",
        "Digital Sciences",
        "Digital Sciences",
        "dsc1",
        "dsc",
        "https://www.th-koeln.de/studium/digital-sciences-master_83002.php",
        "https://www.th-koeln.de/en/academics/digital-sciences-masters-program_83005.php",
        msc,
        Person.Single(
          "ald",
          "Dobrynin",
          "Alexander",
          "M.Sc.",
          List(f10),
          "ad",
          PersonStatus.Active
        ),
        LocalDate.of(2027, 9, 30),
        List(
          StudyForm(
            StudyFormType("full", "", ""),
            30,
            List(
              StudyFormScope(3, 90, "", ""),
              StudyFormScope(4, 120, "a", "b")
            )
          )
        ),
        List(Language("en", "Englisch", "--")),
        List(
          Season("ws", "Wintersemester", "--"),
          Season("ss", "Sommersemester", "--")
        ),
        List(
          Location("dz", "Deutz", "--"),
          Location("gm", "Gummersbach", "--")
        ),
        RestrictedAdmission(value = true, "Orts-NC", "local nc"),
        "a b\n",
        "a",
        "a\n",
        "b"
      )
      assert(sp1.abbrev == esp1.abbrev)
      assert(sp1.deLabel == esp1.deLabel)
      assert(sp1.enLabel == esp1.enLabel)
      assert(sp1.internalAbbreviation == esp1.internalAbbreviation)
      assert(sp1.externalAbbreviation == esp1.externalAbbreviation)
      assert(sp1.deUrl == esp1.deUrl)
      assert(sp1.enUrl == esp1.enUrl)
      assert(sp1.grade == esp1.grade)
      assert(sp1.programDirector == esp1.programDirector)
      assert(sp1.accreditationUntil == esp1.accreditationUntil)
      assert(sp1.studyForm == esp1.studyForm)
      assert(sp1.language == esp1.language)
      assert(sp1.seasons == esp1.seasons)
      assert(sp1.campus == esp1.campus)
      assert(sp1.restrictedAdmission == esp1.restrictedAdmission)
      assert(sp1.deDescription == esp1.deDescription)
      assert(sp1.deNote == esp1.deNote)
      assert(sp1.enDescription == esp1.enDescription)
      assert(sp1.enNote == esp1.enNote)

      val sp2 = res.value(1)
      val esp2 = basedata.StudyProgram(
        "inf_inf",
        "Informatik",
        "Computer Science",
        "inf1",
        "inf1",
        "https://www.th-koeln.de/studium/informatik-bachelor_3488.php",
        "https://www.th-koeln.de/en/academics/computer-science-bachelors-program_7326.php",
        bsc,
        Person.Single(
          "ald",
          "Dobrynin",
          "Alexander",
          "M.Sc.",
          List(f10),
          "ad",
          PersonStatus.Active
        ),
        LocalDate.of(2026, 9, 30),
        List(
          StudyForm(
            StudyFormType("full", "", ""),
            30,
            List(StudyFormScope(7, 210, "Praxissemester", "Internship"))
          )
        ),
        List(Language("de", "Deutsch", "--")),
        List(Season("ws", "Wintersemester", "--")),
        List(Location("gm", "Gummersbach", "--")),
        RestrictedAdmission(value = true, "Orts-NC", "local nc"),
        "a\n",
        "",
        "b\n",
        ""
      )
      assert(sp2.abbrev == esp2.abbrev)
      assert(sp2.deLabel == esp2.deLabel)
      assert(sp2.enLabel == esp2.enLabel)
      assert(sp2.internalAbbreviation == esp2.internalAbbreviation)
      assert(sp2.externalAbbreviation == esp2.externalAbbreviation)
      assert(sp2.deUrl == esp2.deUrl)
      assert(sp2.enUrl == esp2.enUrl)
      assert(sp2.grade == esp2.grade)
      assert(sp2.programDirector == esp2.programDirector)
      assert(sp2.accreditationUntil == esp2.accreditationUntil)
      assert(sp2.studyForm == esp2.studyForm)
      assert(sp2.language == esp2.language)
      assert(sp2.seasons == esp2.seasons)
      assert(sp2.campus == esp2.campus)
      assert(sp2.restrictedAdmission == esp2.restrictedAdmission)
      assert(sp2.deDescription == esp2.deDescription)
      assert(sp2.deNote == esp2.deNote)
      assert(sp2.enDescription == esp2.enDescription)
      assert(sp2.enNote == esp2.enNote)

      val sp3 = res.value(2)
      val esp3 = basedata.StudyProgram(
        "ing_gme",
        "Allgemeiner Maschinenbau",
        "General Mechanical Engineering",
        "gme",
        "gme",
        "https://www.th-koeln.de/studium/allgemeiner-maschinenbau-bachelor_2709.php",
        "https://www.th-koeln.de/en/academics/general-mechanical-engineering-bachelors-program_7323.php",
        beng,
        Person.Single(
          "ald",
          "Dobrynin",
          "Alexander",
          "M.Sc.",
          List(f10),
          "ad",
          PersonStatus.Active
        ),
        LocalDate.of(2026, 9, 30),
        List(
          StudyForm(
            StudyFormType("full", "", ""),
            30,
            List(
              StudyFormScope(6, 180, "", ""),
              StudyFormScope(
                7,
                210,
                "Im Falle eines integrierten fakultativen Praxissemester",
                "Internship"
              )
            )
          ),
          StudyForm(
            StudyFormType("part", "", ""),
            30,
            List(
              StudyFormScope(9, 180, "", ""),
              StudyFormScope(
                10,
                210,
                "Im Falle eines integrierten fakultativen Praxissemester",
                "Internship"
              )
            )
          )
        ),
        List(Language("de", "Deutsch", "--")),
        List(
          Season("ws", "Wintersemester", "--"),
          Season("ss", "Sommersemester", "--")
        ),
        List(Location("gm", "Gummersbach", "--")),
        RestrictedAdmission(value = false, "", ""),
        "a",
        "b\nc\nd\n",
        "e",
        "f\ng\nh\n"
      )
      assert(sp3.abbrev == esp3.abbrev)
      assert(sp3.deLabel == esp3.deLabel)
      assert(sp3.enLabel == esp3.enLabel)
      assert(sp3.internalAbbreviation == esp3.internalAbbreviation)
      assert(sp3.externalAbbreviation == esp3.externalAbbreviation)
      assert(sp3.deUrl == esp3.deUrl)
      assert(sp3.enUrl == esp3.enUrl)
      assert(sp3.grade == esp3.grade)
      assert(sp3.programDirector == esp3.programDirector)
      assert(sp3.accreditationUntil == esp3.accreditationUntil)
      assert(sp3.studyForm == esp3.studyForm)
      assert(sp3.language == esp3.language)
      assert(sp3.seasons == esp3.seasons)
      assert(sp3.campus == esp3.campus)
      assert(sp3.restrictedAdmission == esp3.restrictedAdmission)
      assert(sp3.deDescription == esp3.deDescription)
      assert(sp3.deNote == esp3.deNote)
      assert(sp3.enDescription == esp3.enDescription)
      assert(sp3.enNote == esp3.enNote)
      assert(rest.isEmpty)

      val sp4 = res.value(3)
      val esp4 = basedata.StudyProgram(
        "ing_ait",
        "Automation & IT",
        "Automation & IT",
        "ait",
        "ait",
        "https://www.th-koeln.de/studium/automation--it-master_3429.php",
        "https://www.th-koeln.de/en/academics/automation--it-master_6815.php",
        meng,
        Person.Single(
          "ald",
          "Dobrynin",
          "Alexander",
          "M.Sc.",
          List(f10),
          "ad",
          PersonStatus.Active
        ),
        LocalDate.of(2026, 9, 30),
        List(
          StudyForm(
            StudyFormType("full", "", ""),
            30,
            List(StudyFormScope(4, 120, "", ""))
          ),
          StudyForm(
            StudyFormType("part", "", ""),
            30,
            List(StudyFormScope(6, 120, "", ""))
          )
        ),
        List(Language("en", "Englisch", "--")),
        List(Season("ws", "Wintersemester", "--")),
        List(Location("gm", "Gummersbach", "--")),
        RestrictedAdmission(value = false, "", ""),
        "a\n",
        "b\n",
        "c\n",
        "d\n"
      )
      assert(sp4.abbrev == esp4.abbrev)
      assert(sp4.deLabel == esp4.deLabel)
      assert(sp4.enLabel == esp4.enLabel)
      assert(sp4.internalAbbreviation == esp4.internalAbbreviation)
      assert(sp4.externalAbbreviation == esp4.externalAbbreviation)
      assert(sp4.deUrl == esp4.deUrl)
      assert(sp4.enUrl == esp4.enUrl)
      assert(sp4.grade == esp4.grade)
      assert(sp4.programDirector == esp4.programDirector)
      assert(sp4.accreditationUntil == esp4.accreditationUntil)
      assert(sp4.studyForm == esp4.studyForm)
      assert(sp4.language == esp4.language)
      assert(sp4.seasons == esp4.seasons)
      assert(sp4.campus == esp4.campus)
      assert(sp4.restrictedAdmission == esp4.restrictedAdmission)
      assert(sp4.deDescription == esp4.deDescription)
      assert(sp4.deNote == esp4.deNote)
      assert(sp4.enDescription == esp4.enDescription)
      assert(sp4.enNote == esp4.enNote)
      assert(rest.isEmpty)
    }

    "parse dsi" in {
      val (res, rest) = withFile0("test/parsing/res/program.dsi.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.nonEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 1)
      assert(sp.studyForm.head.scope.size == 2)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 2)
      assert(sp.campus.size == 2)
      assert(sp.restrictedAdmission.deReason.nonEmpty)
      assert(sp.restrictedAdmission.enReason.nonEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.isEmpty)
      assert(sp.enDescription.nonEmpty)
      assert(sp.enNote.isEmpty)
      assert(rest.isEmpty)
    }

    "parse inf" in {
      val (res, rest) = withFile0("test/parsing/res/program.inf.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 1)
      assert(sp.studyForm.head.scope.size == 1)
      assert(sp.studyForm.head.scope.head.deReason.nonEmpty)
      assert(sp.studyForm.head.scope.head.enReason.nonEmpty)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 1)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.nonEmpty)
      assert(sp.restrictedAdmission.enReason.nonEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.isEmpty)
      assert(sp.enDescription.nonEmpty)
      assert(sp.enNote.isEmpty)
      assert(rest.isEmpty)
    }

    "parse gme" in {
      val (res, rest) = withFile0("test/parsing/res/program.gme.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 2)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 2)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.isEmpty)
      assert(sp.restrictedAdmission.enReason.isEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.nonEmpty)
      assert(sp.enDescription.nonEmpty)
      assert(sp.enNote.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse ait" in {
      val (res, rest) = withFile0("test/parsing/res/program.ait.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 2)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 1)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.isEmpty)
      assert(sp.restrictedAdmission.enReason.isEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.nonEmpty)
      assert(sp.enDescription.nonEmpty)
      assert(sp.enNote.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse coco" in {
      val (res, rest) = withFile0("test/parsing/res/program.coco.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.isEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 1)
      assert(sp.studyForm.head.scope.size == 2)
      assert(sp.studyForm.head.scope(1).deReason.nonEmpty)
      assert(sp.studyForm.head.scope(1).enReason.nonEmpty)
      assert(sp.language.size == 2)
      assert(sp.seasons.size == 1)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.isEmpty)
      assert(sp.restrictedAdmission.enReason.isEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.isEmpty)
      assert(sp.enDescription.isEmpty)
      assert(sp.enNote.isEmpty)
      assert(rest.isEmpty)
    }

    "parse een" in {
      val (res, rest) = withFile0("test/parsing/res/program.een.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 2)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 2)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.isEmpty)
      assert(sp.restrictedAdmission.enReason.isEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.nonEmpty)
      assert(sp.enDescription.isEmpty)
      assert(sp.enNote.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse itm" in {
      val (res, rest) = withFile0("test/parsing/res/program.itm.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 1)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 1)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.nonEmpty)
      assert(sp.restrictedAdmission.enReason.nonEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.isEmpty)
      assert(sp.enDescription.isEmpty)
      assert(sp.enNote.isEmpty)
      assert(rest.isEmpty)
    }

    "parse mi" in {
      val (res, rest) = withFile0("test/parsing/res/program.mi.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 1)
      assert(sp.studyForm.head.scope.size == 1)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 1)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.nonEmpty)
      assert(sp.restrictedAdmission.enReason.nonEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.isEmpty)
      assert(sp.enDescription.nonEmpty)
      assert(sp.enNote.isEmpty)
      assert(rest.isEmpty)
    }

    "parse mim" in {
      val (res, rest) = withFile0("test/parsing/res/program.mim.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 1)
      assert(sp.studyForm.head.scope.size == 1)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 2)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.nonEmpty)
      assert(sp.restrictedAdmission.enReason.nonEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.isEmpty)
      assert(sp.enDescription.nonEmpty)
      assert(sp.enNote.isEmpty)
      assert(rest.isEmpty)
    }

    "parse pdpd" in {
      val (res, rest) = withFile0("test/parsing/res/program.pdpd.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 1)
      assert(sp.studyForm.head.scope.size == 2)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 1)
      assert(sp.campus.size == 2)
      assert(sp.restrictedAdmission.deReason.isEmpty)
      assert(sp.restrictedAdmission.enReason.isEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.isEmpty)
      assert(sp.enDescription.nonEmpty)
      assert(sp.enNote.isEmpty)
      assert(rest.isEmpty)
    }

    "parse wsc" in {
      val (res, rest) = withFile0("test/parsing/res/program.wsc.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.isEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 1)
      assert(sp.studyForm.head.scope.size == 1)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 2)
      assert(sp.campus.size == 2)
      assert(sp.restrictedAdmission.deReason.isEmpty)
      assert(sp.restrictedAdmission.enReason.isEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.nonEmpty)
      assert(sp.enDescription.isEmpty)
      assert(sp.enNote.isEmpty)
      assert(rest.isEmpty)
    }

    "parse wi" in {
      val (res, rest) = withFile0("test/parsing/res/program.wi.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 1)
      assert(sp.studyForm.head.scope.size == 1)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 1)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.nonEmpty)
      assert(sp.restrictedAdmission.enReason.nonEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.isEmpty)
      assert(sp.enDescription.nonEmpty)
      assert(sp.enNote.isEmpty)
      assert(rest.isEmpty)
    }

    "parse wiv" in {
      val (res, rest) = withFile0("test/parsing/res/program.wiv.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 1)
      assert(sp.studyForm.head.scope.size == 1)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 1)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.nonEmpty)
      assert(sp.restrictedAdmission.enReason.nonEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.nonEmpty)
      assert(sp.enDescription.nonEmpty)
      assert(sp.enNote.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse wivm" in {
      val (res, rest) = withFile0("test/parsing/res/program.wivm.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 1)
      assert(sp.studyForm.head.scope.size == 1)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 1)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.nonEmpty)
      assert(sp.restrictedAdmission.enReason.nonEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.nonEmpty)
      assert(sp.enDescription.nonEmpty)
      assert(sp.enNote.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse wiw" in {
      val (res, rest) = withFile0("test/parsing/res/program.wiw.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 2)
      assert(sp.studyForm.head.scope.size == 2)
      assert(sp.studyForm.last.scope.size == 2)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 2)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.isEmpty)
      assert(sp.restrictedAdmission.enReason.isEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.nonEmpty)
      assert(sp.enDescription.nonEmpty)
      assert(sp.enNote.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse wiwm" in {
      val (res, rest) = withFile0("test/parsing/res/program.wiwm.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.abbrev.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.deUrl.nonEmpty)
      assert(sp.enUrl.nonEmpty)
      assert(sp.grade.abbrev.nonEmpty)
      assert(sp.programDirector.id.nonEmpty)
      assert(sp.studyForm.size == 1)
      assert(sp.studyForm.head.scope.size == 2)
      assert(sp.language.size == 1)
      assert(sp.seasons.size == 2)
      assert(sp.campus.size == 1)
      assert(sp.restrictedAdmission.deReason.isEmpty)
      assert(sp.restrictedAdmission.enReason.isEmpty)
      assert(sp.deDescription.nonEmpty)
      assert(sp.deNote.isEmpty)
      assert(sp.enDescription.nonEmpty)
      assert(sp.enNote.isEmpty)
      assert(rest.isEmpty)
    }
  }
}
