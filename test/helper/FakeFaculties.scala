package helper

import models.core.Faculty

trait FakeFaculties {
  val f10 = Faculty("f10", "10", "10")
  val f03 = Faculty("f03", "03", "03")

  implicit def fakeFaculties: Seq[Faculty] =
    Seq(f10, f03)
}
