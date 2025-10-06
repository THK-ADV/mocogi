package printing

import java.util.UUID

import cats.data.NonEmptyList
import models.*
import org.scalatest.wordspec.AnyWordSpec
import parsing.metadata.VersionScheme
import parsing.types.ModuleParticipants
import printing.yaml.MetadataYamlPrinter

final class MetadataYamlPrinterSpec extends AnyWordSpec with PrinterSpec {

  val printer = new MetadataYamlPrinter(2)
  val m1      = UUID.fromString("0ef7d976-206e-4654-a987-8d0e184fdd3f")
  val m2      = UUID.fromString("330bd356-e766-433b-ae2c-0a98d49ca49d")

  "A MetadataProtocolPrinter" should {
    "print version scheme" in {
      val version: VersionScheme = VersionScheme(1, "s")
      assert(run(printer.versionScheme(version)) === "v1.0s")
    }

    "print opener" in {
      val version: VersionScheme = VersionScheme(1, "s")
      assert(run(printer.opener(version)) === "---v1.0s\n")
    }

    "print title" in {
      assert(run(printer.title("Title")) === "title: Title\n")
    }

    "print abbreviation" in {
      assert(run(printer.abbreviation("ABC")) === "abbreviation: ABC\n")
    }

    "print module type" in {
      assert(run(printer.moduleType("module")) === "type: type.module\n")
    }

    "print module relation" in {
      val parent0 = ModuleRelationProtocol.Parent(NonEmptyList.one(m1))
      val res0    = s"relation:\n  children: module.$m1\n"
      assert(run(printer.moduleRelation(parent0)) === res0)
      val parent1 = ModuleRelationProtocol.Parent(NonEmptyList.of(m1, m2))
      val res1 =
        s"""relation:
           |  children:
           |    - module.$m1
           |    - module.$m2\n""".stripMargin
      assert(run(printer.moduleRelation(parent1)) === res1)
      val child = ModuleRelationProtocol.Child(m1)
      val res2  = s"relation:\n  parent: module.$m1\n"
      assert(run(printer.moduleRelation(child)) === res2)
    }

    "print ects" in {
      assert(run(printer.ects(5)) === "ects: 5.0\n")
    }

    "print language" in {
      assert(run(printer.language("de")) === "language: lang.de\n")
    }

    "print duration" in {
      assert(run(printer.duration(1)) === "duration: 1\n")
    }

    "print frequency" in {
      assert(run(printer.frequency("ws")) === "frequency: season.ws\n")
    }

    "print responsibilities" in {
      val moduleManagement1 = NonEmptyList.one("ald")
      val lecturers1        = NonEmptyList.of("ald", "abe")
      val res1 =
        s"""responsibilities:
           |  module_management: person.ald
           |  lecturers:
           |    - person.abe
           |    - person.ald\n""".stripMargin
      assert(
        run(printer.responsibilities(moduleManagement1, lecturers1)) === res1
      )
      val moduleManagement2 = NonEmptyList.of("ald", "abe")
      val res2 =
        s"""responsibilities:
           |  module_management:
           |    - person.abe
           |    - person.ald
           |  lecturers:
           |    - person.abe
           |    - person.ald\n""".stripMargin
      assert(
        run(printer.responsibilities(moduleManagement2, lecturers1)) === res2
      )
    }

    "print assessment methods mandatory" in {
      val values1 = NonEmptyList.of(
        ModuleAssessmentMethodEntryProtocol("project", None, Nil),
        ModuleAssessmentMethodEntryProtocol("exam", None, Nil)
      )
      val res1 =
        s"""assessment_methods_mandatory:
           |  - method: assessment.exam
           |  - method: assessment.project\n""".stripMargin
      assert(run(printer.assessmentMethodsMandatory(values1)) == res1)
      val values2 = NonEmptyList.of(
        ModuleAssessmentMethodEntryProtocol("exam", Some(100), Nil),
        ModuleAssessmentMethodEntryProtocol("project", None, Nil)
      )
      val res2 =
        s"""assessment_methods_mandatory:
           |  - method: assessment.exam
           |    percentage: 100.0
           |  - method: assessment.project\n""".stripMargin
      assert(run(printer.assessmentMethodsMandatory(values2)) == res2)
      val values3 = NonEmptyList.of(
        ModuleAssessmentMethodEntryProtocol("project", None, Nil),
        ModuleAssessmentMethodEntryProtocol(
          "exam",
          Some(100),
          List("def", "abc")
        )
      )
      val res3 =
        s"""assessment_methods_mandatory:
           |  - method: assessment.exam
           |    percentage: 100.0
           |    precondition:
           |      - assessment.abc
           |      - assessment.def
           |  - method: assessment.project\n""".stripMargin
      assert(run(printer.assessmentMethodsMandatory(values3)) == res3)
    }

    "print workload" in {
      val workload = ModuleWorkload(1, 2, 3, 4, 5, 6)
      val res =
        s"""workload:
           |  lecture: 1
           |  seminar: 2
           |  practical: 3
           |  exercise: 4
           |  project_supervision: 5
           |  project_work: 6\n""".stripMargin
      assert(run(printer.workload(workload)) == res)
    }

    "print recommended prerequisites" in {
      val entry0 = ModulePrerequisiteEntryProtocol("", Nil, Nil)
      val res0   = ""
      assert(run(printer.recommendedPrerequisites(entry0)) == res0)

      val entry1 = ModulePrerequisiteEntryProtocol("abc", Nil, Nil)
      val res1 =
        s"""recommended_prerequisites:
           |  text: abc\n""".stripMargin
      assert(run(printer.recommendedPrerequisites(entry1)) == res1)

      val entry2 = ModulePrerequisiteEntryProtocol("abc", List(m2, m1), Nil)
      val res2 =
        s"""recommended_prerequisites:
           |  text: abc
           |  modules:
           |    - module.$m1
           |    - module.$m2\n""".stripMargin
      assert(run(printer.recommendedPrerequisites(entry2)) == res2)

      val entry3 =
        ModulePrerequisiteEntryProtocol("abc", List(m1, m2), List("def", "abc"))
      val res3 =
        s"""recommended_prerequisites:
           |  text: abc
           |  modules:
           |    - module.$m1
           |    - module.$m2
           |  study_programs:
           |    - study_program.abc
           |    - study_program.def\n""".stripMargin
      assert(run(printer.recommendedPrerequisites(entry3)) == res3)

      val entry4 =
        ModulePrerequisiteEntryProtocol("abc", Nil, List("abc", "def"))
      val res4 =
        s"""recommended_prerequisites:
           |  text: abc
           |  study_programs:
           |    - study_program.abc
           |    - study_program.def\n""".stripMargin
      assert(run(printer.recommendedPrerequisites(entry4)) == res4)

      val entry5 = ModulePrerequisiteEntryProtocol("", Nil, List("abc", "def"))
      val res5 =
        s"""recommended_prerequisites:
           |  study_programs:
           |    - study_program.abc
           |    - study_program.def\n""".stripMargin
      assert(run(printer.recommendedPrerequisites(entry5)) == res5)
    }

    "print status" in {
      assert(run(printer.status("active")) == "status: status.active\n")
    }

    "print location" in {
      assert(run(printer.location("active")) == "location: location.active\n")
    }

    "print participants" in {
      val participants = ModuleParticipants(0, 10)
      val res =
        s"""participants:
           |  min: 0
           |  max: 10\n""".stripMargin
      assert(run(printer.participants(participants)) == res)
    }

    "print taught with" in {
      val taughtWith = NonEmptyList.of(m1, m2)
      val res =
        s"""taught_with:
           |  - module.$m1
           |  - module.$m2\n""".stripMargin
      assert(run(printer.taughtWith(taughtWith)) == res)
    }

    "print po mandatory" in {
      val po1 = NonEmptyList.of(
        ModulePOMandatoryProtocol("abc", None, List(1)),
        ModulePOMandatoryProtocol("ghi", Some("foo"), List(1, 2)),
        ModulePOMandatoryProtocol("def", None, List(2, 1))
      )
      val res1 =
        s"""po_mandatory:
           |  - study_program: study_program.abc
           |    recommended_semester: 1
           |  - study_program: study_program.def
           |    recommended_semester:
           |      - 1
           |      - 2
           |  - study_program: study_program.ghi.foo
           |    recommended_semester:
           |      - 1
           |      - 2\n""".stripMargin
      assert(run(printer.poMandatory(po1)) == res1)
    }

    "print po optional" in {
      val po1 = NonEmptyList.of(
        ModulePOOptionalProtocol(
          "abc",
          None,
          m1,
          partOfCatalog = true,
          List(1)
        ),
        models.ModulePOOptionalProtocol(
          "def",
          Some("foo"),
          m1,
          partOfCatalog = false,
          List(2, 1)
        )
      )
      val res1 =
        s"""po_optional:
           |  - study_program: study_program.abc
           |    instance_of: module.$m1
           |    part_of_catalog: true
           |    recommended_semester: 1
           |  - study_program: study_program.def.foo
           |    instance_of: module.$m1
           |    part_of_catalog: false
           |    recommended_semester:
           |      - 1
           |      - 2\n""".stripMargin
      assert(run(printer.poOptional(po1)) == res1)
    }

    "print examiner" in {
      val examiner = Examiner("ald", "abe")
      assert(
        run(
          printer.examiner(examiner)
        ) === "first_examiner: person.ald\nsecond_examiner: person.abe\n"
      )
    }

    "print exam phases" in {
      val examPhases = NonEmptyList.of("b", "a")
      val input      = printer.examPhases(examPhases)
      val output     = "exam_phases:\n  - exam_phase.a\n  - exam_phase.b\n"
      assert(run(input) == output)
    }

    "print attendance requirement" in {
      val att   = AttendanceRequirement("min", "reason", "absence")
      val input = printer.attendanceRequirement(att)
      val output =
        """attendance_requirement:
          |  min: min
          |  reason: reason
          |  absence: absence
          |""".stripMargin
      assert(run(input) == output)
    }

    "print assessment prerequisite" in {
      val ass   = AssessmentPrerequisite("modules", "reason")
      val input = printer.assessmentPrerequisite(ass)
      val output =
        """assessment_prerequisite:
          |  modules: modules
          |  reason: reason
          |""".stripMargin
      assert(run(input) == output)
    }

    "print" in {
      val metadata = models.MetadataProtocol(
        "Module A",
        "M",
        "module",
        5.0,
        "de",
        1,
        "ws",
        ModuleWorkload(10, 10, 10, 10, 10, 10),
        "active",
        "gm",
        Some(ModuleParticipants(0, 10)),
        Some(
          ModuleRelationProtocol.Parent(NonEmptyList.of(m1, m2))
        ),
        NonEmptyList.one("ald"),
        NonEmptyList.of("ald", "abe"),
        ModuleAssessmentMethodsProtocol(
          List(
            ModuleAssessmentMethodEntryProtocol(
              "written-exam",
              Some(100),
              List("practical")
            )
          )
        ),
        Examiner("ald", "abe"),
        NonEmptyList.of("a", "b"),
        ModulePrerequisitesProtocol(
          Some(
            ModulePrerequisiteEntryProtocol("abc", List(m1), Nil)
          ),
          Some(
            ModulePrerequisiteEntryProtocol("", Nil, List("po1", "po2"))
          )
        ),
        ModulePOProtocol(
          List(
            ModulePOMandatoryProtocol("po1", None, List(1, 2)),
            ModulePOMandatoryProtocol("po2", None, List(1)),
            ModulePOMandatoryProtocol("po3", None, List(1))
          ),
          List(
            models.ModulePOOptionalProtocol(
              "po4",
              None,
              m1,
              partOfCatalog = false,
              List(1, 2)
            ),
            models.ModulePOOptionalProtocol(
              "po5",
              None,
              m2,
              partOfCatalog = true,
              Nil
            )
          )
        ),
        List(m1),
        None,
        Some(AssessmentPrerequisite("modules", "reason"))
      )
      val id                     = UUID.randomUUID
      val version: VersionScheme = VersionScheme(1, "s")
      val print = printer
        .printer(version)
        .print((id, metadata), new StringBuilder())
        .value
        .toString()
      val res =
        s"""---v1.0s
           |id: $id
           |title: Module A
           |abbreviation: M
           |type: type.module
           |relation:
           |  children:
           |    - module.$m1
           |    - module.$m2
           |ects: 5.0
           |language: lang.de
           |duration: 1
           |frequency: season.ws
           |responsibilities:
           |  module_management: person.ald
           |  lecturers:
           |    - person.abe
           |    - person.ald
           |assessment_methods_mandatory:
           |  - method: assessment.written-exam
           |    percentage: 100.0
           |    precondition: assessment.practical
           |first_examiner: person.ald
           |second_examiner: person.abe
           |exam_phases:
           |  - exam_phase.a
           |  - exam_phase.b
           |workload:
           |  lecture: 10
           |  seminar: 10
           |  practical: 10
           |  exercise: 10
           |  project_supervision: 10
           |  project_work: 10
           |recommended_prerequisites:
           |  text: abc
           |  modules: module.$m1
           |required_prerequisites:
           |  study_programs:
           |    - study_program.po1
           |    - study_program.po2
           |status: status.active
           |location: location.gm
           |po_mandatory:
           |  - study_program: study_program.po1
           |    recommended_semester:
           |      - 1
           |      - 2
           |  - study_program: study_program.po2
           |    recommended_semester: 1
           |  - study_program: study_program.po3
           |    recommended_semester: 1
           |po_optional:
           |  - study_program: study_program.po4
           |    instance_of: module.$m1
           |    part_of_catalog: false
           |    recommended_semester:
           |      - 1
           |      - 2
           |  - study_program: study_program.po5
           |    instance_of: module.$m2
           |    part_of_catalog: true
           |participants:
           |  min: 0
           |  max: 10
           |taught_with: module.$m1
           |assessment_prerequisite:
           |  modules: modules
           |  reason: reason
           |---""".stripMargin
      assert(print == res)
    }
  }
}
