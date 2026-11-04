package service.artifact.examlist

import java.time.LocalDate
import java.util.UUID

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class ExamListServiceSpec extends AnyFunSuite with Matchers {

  private val moduleA = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val moduleB = UUID.fromString("00000000-0000-0000-0000-000000000002")
  private val moduleC = UUID.fromString("00000000-0000-0000-0000-000000000003")
  private val generic = UUID.fromString("00000000-0000-0000-0000-000000000004")
  private val unknown = UUID.fromString("00000000-0000-0000-0000-000000000009")

  private val wiseStart = LocalDate.of(2026, 11, 2)
  private val wiseEnd   = LocalDate.of(2026, 11, 20)
  private val offStart  = LocalDate.of(2027, 2, 1)
  private val offEnd    = LocalDate.of(2027, 2, 12)
  private val wise      = ExamListPeriod(wiseStart, wiseEnd)
  private val off       = ExamListPeriod(offStart, offEnd)

  private val config = ExamListConfig(
    semester = "wise_2026",
    date = None,
    examPhases = List("wise_1", "off_wise"),
    excludeGenericModules = false,
    excludedModuleIds = Nil,
    excludedExams = Nil,
    registrationPeriods = Map(
      "wise_1"   -> DateRange(Some(wiseStart), Some(wiseEnd)),
      "off_wise" -> DateRange(Some(offStart), Some(offEnd))
    ),
    registrationOverrides = Nil
  )

  private def module(
      id: UUID,
      phases: List[String] = List("wise_1"),
      names: Option[List[String]] = None,
      isGeneric: Boolean = false
  ) =
    ExamListModuleOption(
      id,
      s"Module $id",
      "M",
      isGeneric,
      mandatory = true,
      semester = 1,
      Nil,
      Nil,
      "first",
      "second",
      phases,
      names
    )

  private def prepared(options: ExamListModuleOption*)(c: ExamListConfig = config) =
    ExamListService.prepare(c, options).fold(e => fail(e), identity)

  test("a module without exam names yields one default exam, named exams yield one exam each in candidate order") {
    val a = module(moduleA)
    val b = module(moduleB, names = Some(List("Klausur", "Praktikum")))

    prepared(a, b)() shouldBe List(
      PreparedExamListEntry(a, List("wise_1"), List(ExamListExam(None, Map("wise_1" -> wise)))),
      PreparedExamListEntry(
        b,
        List("wise_1"),
        List(
          ExamListExam(Some("Klausur"), Map("wise_1" -> wise)),
          ExamListExam(Some("Praktikum"), Map("wise_1" -> wise))
        )
      )
    )
  }

  test("an override of exam and phase beats the global period without leaking to other exams or phases") {
    val period = ExamListPeriod(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5))
    val m      = module(moduleA, List("wise_1", "off_wise"), Some(List("Klausur", "Praktikum")))
    val c      = config.copy(registrationOverrides =
      List(RegistrationOverride(moduleA, Some("Klausur"), "wise_1", Some(period.start), Some(period.end)))
    )

    prepared(m)(c).flatMap(_.exams) shouldBe List(
      ExamListExam(Some("Klausur"), Map("wise_1" -> period, "off_wise" -> off)),
      ExamListExam(Some("Praktikum"), Map("wise_1" -> wise, "off_wise" -> off))
    )
  }

  test("an override of the default exam applies to it, but not to the same phase of another module") {
    val period = ExamListPeriod(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5))
    val c      = config.copy(registrationOverrides =
      List(RegistrationOverride(moduleA, None, "wise_1", Some(period.start), Some(period.end)))
    )

    prepared(module(moduleA), module(moduleB))(c).map(_.exams) shouldBe List(
      List(ExamListExam(None, Map("wise_1" -> period))),
      List(ExamListExam(None, Map("wise_1" -> wise)))
    )
  }

  test("incomplete and absent periods are all reported together across modules, exams and phases") {
    val c = config.copy(registrationPeriods = Map("wise_1" -> DateRange(Some(wiseStart), None)))
    val a = module(moduleA, List("wise_1", "off_wise"), Some(List("Klausur", "Praktikum")))
    val b = module(moduleB, List("off_wise"))
    val d = module(moduleC, List("wise_1"))

    ExamListService.prepare(c, Seq(a, b, d)) shouldBe Left(
      List(
        s"missing registration period for module ${a.title} ($moduleA), exam Klausur, phase wise_1",
        s"missing registration period for module ${a.title} ($moduleA), exam Klausur, phase off_wise",
        s"missing registration period for module ${a.title} ($moduleA), exam Praktikum, phase wise_1",
        s"missing registration period for module ${a.title} ($moduleA), exam Praktikum, phase off_wise",
        s"missing registration period for module ${b.title} ($moduleB), exam ${b.title}, phase off_wise",
        s"missing registration period for module ${d.title} ($moduleC), exam ${d.title}, phase wise_1"
      ).mkString("; ")
    )
  }

  test("module phases are intersected with the configured phases in module order, modules without any are dropped") {
    val a = module(moduleA, List("off_wise", "sose_1", "wise_1"))
    val b = module(moduleB, List("sose_1", "none"))

    prepared(a, b)() shouldBe List(
      PreparedExamListEntry(
        a,
        List("off_wise", "wise_1"),
        List(ExamListExam(None, Map("off_wise" -> off, "wise_1" -> wise)))
      )
    )
  }

  test("excluded exams are removed, including the default exam, and modules without remaining exams are dropped") {
    val a = module(moduleA, names = Some(List("Klausur", "Praktikum")))
    val b = module(moduleB)
    val d = module(moduleC, names = Some(List("Klausur")))
    val c = config.copy(
      excludedExams =
        List(ExamRef(moduleA, Some("Klausur")), ExamRef(moduleB, None), ExamRef(moduleC, Some("Klausur"))),
      registrationPeriods = Map.empty,
      registrationOverrides =
        List(RegistrationOverride(moduleA, Some("Praktikum"), "wise_1", Some(wiseStart), Some(wiseEnd)))
    )

    prepared(a, b, d)(c) shouldBe List(
      PreparedExamListEntry(a, List("wise_1"), List(ExamListExam(Some("Praktikum"), Map("wise_1" -> wise))))
    )
  }

  test("a default exam exclusion does not match named exams of the same module") {
    val a = module(moduleA, names = Some(List("Klausur")))

    prepared(a)(config.copy(excludedExams = List(ExamRef(moduleA, None)))).flatMap(_.exams.map(_.name)) shouldBe
      List(Some("Klausur"))
  }

  test("excluded modules are dropped without requiring a registration period") {
    val c =
      config.copy(excludedModuleIds = List(moduleA), registrationPeriods = config.registrationPeriods - "off_wise")

    prepared(module(moduleA, List("off_wise")), module(moduleB))(c).map(_.module.id) shouldBe List(moduleB)
  }

  test("generic modules drop the phase none, have no exams and need no registration periods") {
    val g = module(generic, List("none", "wise_1"), Some(List("Klausur")), isGeneric = true)

    prepared(g)(config.copy(registrationPeriods = Map.empty)) shouldBe
      List(PreparedExamListEntry(g, List("wise_1"), Nil))
  }

  test("generic modules are dropped if excluded") {
    val g = module(generic, isGeneric = true)

    prepared(g, module(moduleA))(config.copy(excludeGenericModules = true)).map(_.module.id) shouldBe List(moduleA)
  }

  test("orphaned exclusions and overrides of unknown modules are ignored") {
    val c = config.copy(
      excludedModuleIds = List(unknown),
      excludedExams = List(ExamRef(unknown, None)),
      registrationOverrides = List(RegistrationOverride(unknown, None, "wise_1", None, None))
    )

    prepared(module(moduleA))(c) shouldBe prepared(module(moduleA))()
  }

  test("entries keep the order of the candidates") {
    val ids = List(moduleC, generic, moduleA, moduleB)

    prepared(
      module(moduleC),
      module(generic, isGeneric = true),
      module(moduleA),
      module(moduleB)
    )().map(_.module.id) shouldBe ids
  }
}
