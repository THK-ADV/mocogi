package helper

import java.time.LocalDate

import models.core.PO

trait FakePOs {
  private lazy val ld = LocalDate.of(1998, 5, 9)

  lazy val inf1 = PO("inf1", 0, "", ld, None)

  lazy val wi1 = PO("wi1", 0, "", ld, None)

  lazy val mi1 = PO("mi1", 0, "", ld, None)

  lazy val itm1 = PO("itm1", 0, "", ld, None)

  implicit def fakePOs: Seq[PO] = Seq(inf1, wi1, mi1, itm1)
}
