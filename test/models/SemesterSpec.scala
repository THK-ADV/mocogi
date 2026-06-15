package models

import java.time.LocalDate
import java.time.Month

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

final class SemesterSpec extends AnyWordSpec with Matchers {

  "A Semester" should {

    "build its id from abbrev and year" in {
      Semester.winter(2025).id shouldBe "wise_2025"
      Semester.summer(2026).id shouldBe "sose_2026"
    }

    "expose correct labels and date ranges for winter" in {
      val s = Semester.winter(2025)
      s.abbrev shouldBe "wise"
      s.year shouldBe 2025
      s.deLabel shouldBe "Wintersemester"
      s.enLabel shouldBe "Winter semester"
      s.start shouldBe LocalDate.of(2025, Month.SEPTEMBER, 1)
      s.end shouldBe LocalDate.of(2026, Month.MARCH, 1)
    }

    "expose correct labels and date ranges for summer" in {
      val s = Semester.summer(2026)
      s.abbrev shouldBe "sose"
      s.year shouldBe 2026
      s.deLabel shouldBe "Sommersemester"
      s.enLabel shouldBe "Summer semester"
      s.start shouldBe LocalDate.of(2026, Month.MARCH, 1)
      s.end shouldBe LocalDate.of(2026, Month.SEPTEMBER, 1)
    }
  }

  "Semester.apply" should {

    "parse a winter id" in {
      val s = Semester("wise_2025")
      s.abbrev shouldBe "wise"
      s.year shouldBe 2025
    }

    "parse a summer id" in {
      val s = Semester("sose_2026")
      s.abbrev shouldBe "sose"
      s.year shouldBe 2026
    }

    "round-trip via id" in {
      val s = Semester.winter(2030)
      Semester(s.id).id shouldBe s.id
    }
  }

  "Semester ordering" should {

    "order by year first" in {
      Ordering[Semester].compare(
        Semester.winter(2024),
        Semester.summer(2025)
      ) should be < 0
    }

    "place wise after sose within the same year" in {
      Ordering[Semester].compare(
        Semester.winter(2025),
        Semester.summer(2025)
      ) should be > 0
    }

    "place sose before wise within the same year" in {
      Ordering[Semester].compare(
        Semester.summer(2025),
        Semester.winter(2025)
      ) should be < 0
    }

    "treat identical semesters as equal" in {
      Ordering[Semester].compare(
        Semester.summer(2025),
        Semester.summer(2025)
      ) shouldBe 0
    }

    "sort a mixed list chronologically" in {
      val unsorted = List(
        Semester.winter(2025),
        Semester.summer(2024),
        Semester.summer(2025),
        Semester.winter(2024)
      )
      unsorted.sorted.map(_.id) shouldBe List(
        "sose_2024",
        "wise_2024",
        "sose_2025",
        "wise_2025"
      )
    }
  }

  "Semester.dateRange" should {

    "return [start, end) for winter aligned with the label dates" in {
      val (start, end) = Semester.dateRange("wise_2025")
      start shouldBe LocalDate.of(2025, Month.SEPTEMBER, 1).atStartOfDay
      end shouldBe LocalDate.of(2026, Month.MARCH, 1).atStartOfDay
    }

    "return [start, end) for summer aligned with the label dates" in {
      val (start, end) = Semester.dateRange("sose_2026")
      start shouldBe LocalDate.of(2026, Month.MARCH, 1).atStartOfDay
      end shouldBe LocalDate.of(2026, Month.SEPTEMBER, 1).atStartOfDay
    }

    // This is the partition invariant: consecutive semesters must share
    // identical boundaries, otherwise PostgreSQL range partitions get
    // gaps/overlaps and inserts near the boundary fail.
    "share the boundary between wise and the following sose" in {
      val (_, wiseEnd)   = Semester.dateRange("wise_2025")
      val (soseStart, _) = Semester.dateRange("sose_2026")
      wiseEnd shouldBe soseStart
    }

    "share the boundary between sose and the following wise" in {
      val (_, soseEnd)   = Semester.dateRange("sose_2026")
      val (wiseStart, _) = Semester.dateRange("wise_2026")
      soseEnd shouldBe wiseStart
    }

    "keep boundaries aligned across several consecutive semesters" in {
      val ids = List(
        "sose_2025",
        "wise_2025",
        "sose_2026",
        "wise_2026",
        "sose_2027"
      )
      val ranges = ids.map(Semester.dateRange)
      ranges.sliding(2).foreach {
        case Seq((_, prevEnd), (nextStart, _)) =>
          prevEnd shouldBe nextStart
        case _ => fail("unexpected window size")
      }
    }
  }

  "Semester.of" should {

    "return summer for months March to August" in {
      Semester.of(LocalDate.of(2026, Month.MARCH, 1)).id shouldBe "sose_2026"
      Semester.of(LocalDate.of(2026, Month.AUGUST, 31)).id shouldBe "sose_2026"
    }

    "return the previous year's winter for January and February" in {
      Semester.of(LocalDate.of(2026, Month.JANUARY, 15)).id shouldBe "wise_2025"
      Semester.of(LocalDate.of(2026, Month.FEBRUARY, 28)).id shouldBe "wise_2025"
    }

    "return the current year's winter for September to December" in {
      Semester.of(LocalDate.of(2026, Month.SEPTEMBER, 1)).id shouldBe "wise_2026"
      Semester.of(LocalDate.of(2026, Month.DECEMBER, 31)).id shouldBe "wise_2026"
    }
  }

  "Semester.next" should {

    "return the current year's winter when in summer (March to August)" in {
      Semester.next(LocalDate.of(2026, Month.MARCH, 1)).id shouldBe "wise_2026"
      Semester.next(LocalDate.of(2026, Month.AUGUST, 1)).id shouldBe "wise_2026"
    }

    "return the next year's summer for September to December" in {
      Semester.next(LocalDate.of(2026, Month.SEPTEMBER, 1)).id shouldBe "sose_2027"
      Semester.next(LocalDate.of(2026, Month.DECEMBER, 31)).id shouldBe "sose_2027"
    }

    "return the current year's summer for January and February" in {
      Semester.next(LocalDate.of(2026, Month.JANUARY, 15)).id shouldBe "sose_2026"
      Semester.next(LocalDate.of(2026, Month.FEBRUARY, 28)).id shouldBe "sose_2026"
    }

    "always return the semester following the current one" in {
      val dates = List(
        LocalDate.of(2026, Month.JANUARY, 10),
        LocalDate.of(2026, Month.MAY, 10),
        LocalDate.of(2026, Month.OCTOBER, 10)
      )
      dates.foreach { d =>
        val current = Semester.of(d)
        val nxt     = Semester.next(d)
        Ordering[Semester].compare(nxt, current) should be > 0
      }
    }
  }

  "Semester.currentAndNext" should {

    "return the current semester followed by the next one" in {
      val date = LocalDate.of(2026, Month.MAY, 1)
      Semester.currentAndNext(date).map(_.id) shouldBe List(
        "sose_2026",
        "wise_2026"
      )
    }
  }

  "Semester JSON writes" should {

    "serialize all fields" in {
      val json = Json.toJson(Semester.winter(2025))(Semester.writes)
      (json \ "id").as[String] shouldBe "wise_2025"
      (json \ "abbrev").as[String] shouldBe "wise"
      (json \ "year").as[Int] shouldBe 2025
      (json \ "deLabel").as[String] shouldBe "Wintersemester"
      (json \ "enLabel").as[String] shouldBe "Winter semester"
      (json \ "start").as[String] shouldBe "2025-09-01"
      (json \ "end").as[String] shouldBe "2026-03-01"
    }
  }
}
