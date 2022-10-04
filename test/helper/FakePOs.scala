package helper

import basedata.{PO, StudyProgramPreview}

import java.time.LocalDate

trait FakePOs {
  private lazy val ld = LocalDate.of(1998, 5, 9)

  lazy val inf1 = PO("inf1", 0, ld, ld, None, Nil, StudyProgramPreview(""))

  lazy val wi1 = PO("wi1", 0, ld, ld, None, Nil, StudyProgramPreview(""))

  lazy val mi1 = PO("mi1", 0, ld, ld, None, Nil, StudyProgramPreview(""))

  lazy val itm1 = PO("itm1", 0, ld, ld, None, Nil, StudyProgramPreview(""))

  implicit def fakePOs: Seq[PO] = Seq(inf1, wi1, mi1, itm1)
}
