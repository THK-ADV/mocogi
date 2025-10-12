package modules

import java.time.LocalDate

import models.Semester
import org.scalatest.wordspec.AnyWordSpec

final class SemesterSpec extends AnyWordSpec {
  "A Semester" should {
    "infer the current and next semester based on current date" in {
      def test(semester: List[Semester], cur: String, next: String) = {
        assert(semester.head.id == cur)
        assert(semester(1).id == next)
      }

      val january      = LocalDate.of(2025, 1, 1)
      val february     = january.plusMonths(1)
      val march        = february.plusMonths(1)
      val april        = march.plusMonths(1)
      val may          = april.plusMonths(1)
      val june         = may.plusMonths(1)
      val july         = june.plusMonths(1)
      val august       = july.plusMonths(1)
      val september    = august.plusMonths(1)
      val october      = september.plusMonths(1)
      val november     = october.plusMonths(1)
      val december     = november.plusMonths(1)
      val nextJanuary  = december.plusMonths(1)
      val nextFebruary = nextJanuary.plusMonths(1)

      test(Semester.currentAndNext(january), "wise_2024", "sose_2025")
      test(Semester.currentAndNext(february), "wise_2024", "sose_2025")
      test(Semester.currentAndNext(march), "sose_2025", "wise_2025")
      test(Semester.currentAndNext(april), "sose_2025", "wise_2025")
      test(Semester.currentAndNext(may), "sose_2025", "wise_2025")
      test(Semester.currentAndNext(june), "sose_2025", "wise_2025")
      test(Semester.currentAndNext(july), "sose_2025", "wise_2025")
      test(Semester.currentAndNext(august), "sose_2025", "wise_2025")
      test(Semester.currentAndNext(september), "wise_2025", "sose_2026")
      test(Semester.currentAndNext(october), "wise_2025", "sose_2026")
      test(Semester.currentAndNext(november), "wise_2025", "sose_2026")
      test(Semester.currentAndNext(december), "wise_2025", "sose_2026")
      test(Semester.currentAndNext(nextJanuary), "wise_2025", "sose_2026")
      test(Semester.currentAndNext(nextFebruary), "wise_2025", "sose_2026")
    }
  }
}
