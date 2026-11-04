package service.artifact.examlist

import java.time.LocalDate
import java.util.UUID

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsNull
import play.api.libs.json.JsObject
import play.api.libs.json.Json

final class ExamListConfigSpec extends AnyFunSuite with Matchers {

  private val moduleA = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val unknown = UUID.fromString("00000000-0000-0000-0000-000000000009")
  private val start   = LocalDate.of(2026, 11, 2)
  private val end     = LocalDate.of(2026, 11, 20)

  private val config = ExamListConfig(
    semester = "wise_2026",
    date = Some(LocalDate.of(2026, 10, 1)),
    examPhases = List("wise_1", "off_wise"),
    excludeGenericModules = true,
    excludedModuleIds = List(moduleA),
    excludedExams = List(ExamRef(moduleA, None), ExamRef(moduleA, Some("Klausur"))),
    registrationPeriods = Map("wise_1" -> DateRange(Some(start), Some(end)), "off_wise" -> DateRange(None, None)),
    registrationOverrides = List(RegistrationOverride(moduleA, Some("Klausur"), "wise_1", Some(start), None))
  )

  private def overrideOf(exam: Option[String], phase: String = "wise_1", s: LocalDate = start, e: LocalDate = end) =
    RegistrationOverride(moduleA, exam, phase, Some(s), Some(e))

  private def label(exam: String, phase: String = "wise_1") =
    s"registration override for module $moduleA, exam $exam, phase $phase"

  test("incomplete periods and orphaned exclusions and overrides are no errors") {
    val orphaned = config.copy(
      excludedModuleIds = List(unknown),
      excludedExams = List(ExamRef(unknown, None)),
      registrationOverrides = List(RegistrationOverride(unknown, None, "off_wise", None, Some(end)))
    )

    ExamListConfig.errors(config) shouldBe Nil
    ExamListConfig.errors(orphaned) shouldBe Nil
  }

  for semester <- Seq("wise_26", "summer_2026", "sose_2026 ", "WISE_2026") do
    test(s"semester '$semester' is rejected") {
      ExamListConfig.errors(config.copy(semester = semester)) shouldBe List(s"invalid semester $semester")
    }

  test("unknown phases and none are rejected in phases, periods and overrides") {
    val c = config.copy(
      examPhases = List("wise_1", "none", "winter"),
      registrationPeriods = Map("none" -> DateRange(None, None)),
      registrationOverrides = List(overrideOf(None, "wise_2"))
    )

    ExamListConfig.errors(c) shouldBe List(
      "unknown exam phase none",
      "unknown exam phase winter",
      "unknown exam phase none of a registration period",
      s"${label("default", "wise_2")} has an unknown phase"
    )
  }

  test("reversed periods and overrides are rejected, a single-day range is allowed") {
    val c = config.copy(
      registrationPeriods =
        Map("wise_1" -> DateRange(Some(end), Some(start)), "off_wise" -> DateRange(Some(end), Some(end))),
      registrationOverrides =
        List(overrideOf(Some("Klausur"), s = end, e = start), overrideOf(None, s = start, e = start))
    )

    ExamListConfig.errors(c) shouldBe List(
      "registration period of phase wise_1 starts after it ends",
      s"${label("Klausur")} starts after it ends"
    )
  }

  test("an override defined several times for the same exam and phase is reported once") {
    val c = config.copy(registrationOverrides =
      List(overrideOf(Some("Klausur")), overrideOf(Some("Klausur")), overrideOf(Some("Klausur")), overrideOf(None))
    )

    ExamListConfig.errors(c) shouldBe List(s"${label("Klausur")} is defined more than once")
  }

  test("overrides of the same module and phase for different exams are no duplicates") {
    val c = config.copy(registrationOverrides =
      List(overrideOf(None), overrideOf(Some("Klausur")), overrideOf(Some("Praktikum")), overrideOf(None, "off_wise"))
    )

    ExamListConfig.errors(c) shouldBe Nil
  }

  test("all errors are reported together") {
    val c = config.copy(
      semester = "wise",
      examPhases = List("x"),
      registrationPeriods = Map("wise_1" -> DateRange(Some(end), Some(start))),
      registrationOverrides = List(overrideOf(None, "y"), overrideOf(None, "y"))
    )

    ExamListConfig.errors(c) should contain theSameElementsAs List(
      "invalid semester wise",
      "unknown exam phase x",
      "registration period of phase wise_1 starts after it ends",
      s"${label("default", "y")} has an unknown phase",
      s"${label("default", "y")} has an unknown phase",
      s"${label("default", "y")} is defined more than once"
    )
  }

  test("JSON writes missing options as null and reads null back to None") {
    val json = Json.toJson(config)

    (json \ "registrationPeriods" \ "off_wise").get shouldBe Json.obj("start" -> JsNull, "end" -> JsNull)
    (json \ "registrationOverrides" \ 0 \ "end").get shouldBe JsNull
    (json \ "excludedExams" \ 0 \ "exam").get shouldBe JsNull
    json.as[ExamListConfig] shouldBe config
    (json.as[JsObject] + ("date" -> JsNull)).as[ExamListConfig] shouldBe config.copy(date = None)
  }
}
