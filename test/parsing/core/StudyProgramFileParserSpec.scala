package parsing.core

import cats.data.NonEmptyList
import helper._
import models.core._
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.core.StudyProgramFileParser._
import parsing.{ParserSpecHelper, withFile0}

final class StudyProgramFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeDegrees
    with FakeIdentities
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

    "parse degree" in {
      val input = "grade: grade.msc"
      val (res, rest) = degreeParser.parse(input)
      assert(res.value == msc)
      assert(rest.isEmpty)
    }

    "parse a single program director" in {
      val input = "program_director: person.ald"
      val (res, rest) = programDirectorParser.parse(input)
      assert(
        res.value == NonEmptyList.one(
          Identity.Person(
            "ald",
            "Dobrynin",
            "Alexander",
            "M.Sc.",
            List(f10),
            "ad",
            "ald",
            PersonStatus.Active
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse a single exam director" in {
      val input = "exam_director: person.ald"
      val (res, rest) = examDirectorParser.parse(input)
      assert(
        res.value == NonEmptyList.one(
          Identity.Person(
            "ald",
            "Dobrynin",
            "Alexander",
            "M.Sc.",
            List(f10),
            "ad",
            "ald",
            PersonStatus.Active
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse multiple program directors" in {
      val input = "program_director:  - person.ald\n  -person.abe"
      val (res, rest) = programDirectorParser.parse(input)
      assert(
        res.value == NonEmptyList.of(
          Identity.Person(
            "ald",
            "Dobrynin",
            "Alexander",
            "M.Sc.",
            List(f10),
            "ad",
            "ald",
            PersonStatus.Active
          ),
          Identity.Person(
            "abe",
            "Bertels",
            "Anja",
            "B.Sc.",
            List(f10),
            "ab",
            "abe",
            PersonStatus.Active
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse multiple exam directors" in {
      val input = "exam_director:  - person.ald\n  -person.abe"
      val (res, rest) = examDirectorParser.parse(input)
      assert(
        res.value == NonEmptyList.of(
          Identity.Person(
            "ald",
            "Dobrynin",
            "Alexander",
            "M.Sc.",
            List(f10),
            "ad",
            "ald",
            PersonStatus.Active
          ),
          Identity.Person(
            "abe",
            "Bertels",
            "Anja",
            "B.Sc.",
            List(f10),
            "ab",
            "abe",
            PersonStatus.Active
          )
        )
      )
      assert(rest.isEmpty)
    }

    "parse study program" in {
      val (res, rest) = withFile0("test/parsing/res/program1.yaml")(
        fileParser.parse
      )
      assert(rest.isEmpty)
      val sp1 = res.value.head
      assert(sp1.id == "inf_dsi")
      assert(sp1.deLabel == "Digital Sciences")
      assert(sp1.enLabel == "Digital Sciences")
      assert(sp1.internalAbbreviation == "dsc1")
      assert(sp1.externalAbbreviation == "dsc")
      assert(sp1.degree == msc.id)
      assert(
        sp1.programDirectors == NonEmptyList.one(
          fakeIdentities.find(_.id == "ald").get.id
        )
      )
      assert(
        sp1.examDirectors == NonEmptyList.one(
          fakeIdentities.find(_.id == "ald").get.id
        )
      )

      val sp2 = res.value(1)
      assert(sp2.id == "inf_inf")
      assert(sp2.deLabel == "Informatik")
      assert(sp2.enLabel == "Computer Science")
      assert(sp2.internalAbbreviation == "inf1")
      assert(sp2.externalAbbreviation == "inf1")
      assert(sp2.degree == bsc.id)
      assert(
        sp2.programDirectors == NonEmptyList
          .fromFoldable(
            fakeIdentities
              .filter(a => a.id == "ald" || a.id == "abe")
              .map(_.id)
          )
          .get
      )
      assert(
        sp2.examDirectors == NonEmptyList
          .fromFoldable(
            fakeIdentities
              .filter(a => a.id == "ald" || a.id == "abe")
              .map(_.id)
          )
          .get
      )

      val sp3 = res.value(2)
      assert(sp3.id == "ing_gme")
      assert(sp3.deLabel == "Allgemeiner Maschinenbau")
      assert(sp3.enLabel == "General Mechanical Engineering")
      assert(sp3.internalAbbreviation == "gme")
      assert(sp3.externalAbbreviation == "gme")
      assert(sp3.degree == beng.id)
      assert(
        sp3.programDirectors == NonEmptyList.one(
          fakeIdentities.find(_.id == "ald").get.id
        )
      )
      assert(
        sp3.examDirectors == NonEmptyList.one(
          fakeIdentities.find(_.id == "abe").get.id
        )
      )

      val sp4 = res.value(3)
      assert(sp4.id == "ing_ait")
      assert(sp4.deLabel == "Automation & IT")
      assert(sp4.enLabel == "Automation & IT")
      assert(sp4.internalAbbreviation == "ait")
      assert(sp4.externalAbbreviation == "ait")
      assert(sp4.degree == meng.id)
      assert(
        sp4.programDirectors == NonEmptyList.one(
          fakeIdentities.find(_.id == "ald").get.id
        )
      )
      assert(
        sp4.examDirectors == NonEmptyList.one(
          fakeIdentities.find(_.id == "ald").get.id
        )
      )
    }

    "parse dsi" in {
      val (res, rest) = withFile0("test/parsing/res/program.dsi.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.nonEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse inf" in {
      val (res, rest) = withFile0("test/parsing/res/program.inf.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse gme" in {
      val (res, rest) = withFile0("test/parsing/res/program.gme.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse ait" in {
      val (res, rest) = withFile0("test/parsing/res/program.ait.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse coco" in {
      val (res, rest) = withFile0("test/parsing/res/program.coco.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse een" in {
      val (res, rest) = withFile0("test/parsing/res/program.een.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse itm" in {
      val (res, rest) = withFile0("test/parsing/res/program.itm.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse mi" in {
      val (res, rest) = withFile0("test/parsing/res/program.mi.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse mim" in {
      val (res, rest) = withFile0("test/parsing/res/program.mim.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse pdpd" in {
      val (res, rest) = withFile0("test/parsing/res/program.pdpd.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse wsc" in {
      val (res, rest) = withFile0("test/parsing/res/program.wsc.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse wi" in {
      val (res, rest) = withFile0("test/parsing/res/program.wi.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse wiv" in {
      val (res, rest) = withFile0("test/parsing/res/program.wiv.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse wivm" in {
      val (res, rest) = withFile0("test/parsing/res/program.wivm.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse wiw" in {
      val (res, rest) = withFile0("test/parsing/res/program.wiw.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }

    "parse wiwm" in {
      val (res, rest) = withFile0("test/parsing/res/program.wiw.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id.nonEmpty)
      assert(sp.deLabel.nonEmpty)
      assert(sp.enLabel.nonEmpty)
      assert(sp.internalAbbreviation.isEmpty)
      assert(sp.externalAbbreviation.isEmpty)
      assert(sp.degree.nonEmpty)
      assert(rest.isEmpty)
    }
  }
}
