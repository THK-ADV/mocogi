package parsing.core

import cats.data.NonEmptyList
import helper._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.core.StudyProgramFileParser._
import parsing.withFile0
import parsing.ParserSpecHelper

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
    "parse dsi" in {
      val (res, rest) = withFile0("test/parsing/res/program.dsi.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "inf_dsi")
      assert(sp.deLabel == "Digital Sciences")
      assert(sp.enLabel == "Digital Sciences")
      assert(sp.degree == msc.id)
      assert(sp.programDirectors == NonEmptyList.of("ald"))
      assert(sp.examDirectors == NonEmptyList.of("ald"))
      assert(rest.isEmpty)
    }

    "parse inf" in {
      val (res, rest) = withFile0("test/parsing/res/program.inf.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "inf_inf")
      assert(sp.deLabel == "Informatik")
      assert(sp.enLabel == "Computer Science")
      assert(sp.degree == bsc.id)
      assert(sp.programDirectors == NonEmptyList.of("ald"))
      assert(sp.examDirectors == NonEmptyList.of("ald"))
      assert(rest.isEmpty)
    }

    "parse gme" in {
      val (res, rest) = withFile0("test/parsing/res/program.gme.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "ing_gme")
      assert(sp.deLabel == "Allgemeiner Maschinenbau")
      assert(sp.enLabel == "General Mechanical Engineering")
      assert(sp.degree == beng.id)
      assert(sp.programDirectors == NonEmptyList.of("ald"))
      assert(sp.examDirectors == NonEmptyList.of("ald", "ddu"))
      assert(rest.isEmpty)
    }

    "parse ait" in {
      val (res, rest) = withFile0("test/parsing/res/program.ait.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "ing_ait")
      assert(sp.deLabel == "Automation & IT")
      assert(sp.enLabel == "Automation & IT")
      assert(sp.degree == meng.id)
      assert(sp.programDirectors == NonEmptyList.of("ald"))
      assert(sp.examDirectors == NonEmptyList.of("ald"))
      assert(rest.isEmpty)
    }

    "parse coco" in {
      val (res, rest) = withFile0("test/parsing/res/program.coco.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "inf_coco")
      assert(sp.deLabel == "Code & Context")
      assert(sp.enLabel == "Code & Context")
      assert(sp.degree == bsc.id)
      assert(sp.programDirectors == NonEmptyList.of("ald"))
      assert(sp.examDirectors == NonEmptyList.of("ald"))
      assert(rest.isEmpty)
    }

    "parse een" in {
      val (res, rest) = withFile0("test/parsing/res/program.een.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "ing_een")
      assert(sp.deLabel == "Elektrotechnik")
      assert(sp.enLabel == "Electrical Engineering")
      assert(sp.degree == beng.id)
      assert(sp.programDirectors == NonEmptyList.of("nn"))
      assert(sp.examDirectors == NonEmptyList.of("nn"))
      assert(rest.isEmpty)
    }

    "parse itm" in {
      val (res, rest) = withFile0("test/parsing/res/program.itm.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "inf_itm")
      assert(sp.deLabel == "IT-Management (Informatik)")
      assert(sp.enLabel == "IT Management (Computer Science)")
      assert(sp.degree == bsc.id)
      assert(sp.programDirectors == NonEmptyList.of("nn"))
      assert(sp.examDirectors == NonEmptyList.of("nn"))
      assert(rest.isEmpty)
    }

    "parse mi" in {
      val (res, rest) = withFile0("test/parsing/res/program.mi.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "inf_mi")
      assert(sp.deLabel == "Medieninformatik")
      assert(sp.enLabel == "Media Informatics")
      assert(sp.degree == bsc.id)
      assert(sp.programDirectors == NonEmptyList.of("nn"))
      assert(sp.examDirectors == NonEmptyList.of("nn"))
      assert(rest.isEmpty)
    }

    "parse mim" in {
      val (res, rest) = withFile0("test/parsing/res/program.mim.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "inf_mim")
      assert(sp.deLabel == "Medieninformatik")
      assert(sp.enLabel == "Media Informatics")
      assert(sp.degree == msc.id)
      assert(sp.programDirectors == NonEmptyList.of("nn"))
      assert(sp.examDirectors == NonEmptyList.of("nn"))
      assert(rest.isEmpty)
    }

    "parse pdpd" in {
      val (res, rest) = withFile0("test/parsing/res/program.pdpd.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "ing_pdpd")
      assert(sp.deLabel == "Produktdesign und Prozessentwicklung")
      assert(sp.enLabel == "Product Design and Process Development")
      assert(sp.degree == msc.id)
      assert(sp.programDirectors == NonEmptyList.of("nn"))
      assert(sp.examDirectors == NonEmptyList.of("nn"))
      assert(rest.isEmpty)
    }

    "parse wsc" in {
      val (res, rest) = withFile0("test/parsing/res/program.wsc.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "inf_wsc")
      assert(sp.deLabel == "Web Science")
      assert(sp.enLabel == "Web Science")
      assert(sp.degree == msc.id)
      assert(sp.programDirectors == NonEmptyList.of("nn"))
      assert(sp.examDirectors == NonEmptyList.of("nn"))
      assert(rest.isEmpty)
    }

    "parse wi" in {
      val (res, rest) = withFile0("test/parsing/res/program.wi.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "inf_wi")
      assert(sp.deLabel == "Wirtschaftsinformatik")
      assert(sp.enLabel == "Business Information Systems")
      assert(sp.degree == bsc.id)
      assert(sp.programDirectors == NonEmptyList.of("nn"))
      assert(sp.examDirectors == NonEmptyList.of("nn"))
      assert(rest.isEmpty)
    }

    "parse wiv" in {
      val (res, rest) = withFile0("test/parsing/res/program.wiv.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "inf_wiv")
      assert(sp.deLabel == "Wirtschaftsinformatik (Verbundstudiengang)")
      assert(
        sp.enLabel == "Business Information Systems (Integrated Campus and Distance Education)"
      )
      assert(sp.degree == bsc.id)
      assert(sp.programDirectors == NonEmptyList.of("nn"))
      assert(sp.examDirectors == NonEmptyList.of("nn"))
      assert(rest.isEmpty)
    }

    "parse wivm" in {
      val (res, rest) = withFile0("test/parsing/res/program.wivm.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "inf_wivm")
      assert(sp.deLabel == "Wirtschaftsinformatik (Verbundstudiengang)")
      assert(
        sp.enLabel == "Business Information Systems (Integrated Campus and Distance Education)"
      )
      assert(sp.degree == msc.id)
      assert(sp.programDirectors == NonEmptyList.of("nn"))
      assert(sp.examDirectors == NonEmptyList.of("nn"))
      assert(rest.isEmpty)
    }

    "parse wiw" in {
      val (res, rest) = withFile0("test/parsing/res/program.wiw.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "ing_wiw")
      assert(sp.deLabel == "Wirtschaftsingenieurwesen")
      assert(sp.enLabel == "Business Administration and Engineering")
      assert(sp.degree == beng.id)
      assert(sp.programDirectors == NonEmptyList.of("nn"))
      assert(sp.examDirectors == NonEmptyList.of("nn"))
      assert(rest.isEmpty)
    }

    "parse wiwm" in {
      val (res, rest) = withFile0("test/parsing/res/program.wiwm.yaml")(
        fileParser.parse
      )
      val sp = res.value.head
      assert(sp.id == "ing_wiwm")
      assert(sp.deLabel == "Wirtschaftsingenieurwesen")
      assert(sp.enLabel == "Business Administration and Engineering")
      assert(sp.degree == msc.id)
      assert(sp.programDirectors == NonEmptyList.of("nn"))
      assert(sp.examDirectors == NonEmptyList.of("nn"))
      assert(rest.isEmpty)
    }
  }
}
