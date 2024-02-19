package helper

import models.core.Degree

trait FakeDegrees {
  lazy val bsc = Degree(
    "bsc",
    "B.Sc.",
    "Bachelor of Science",
    "B.Sc.",
    "Bachelor of Science"
  )

  lazy val msc =
    Degree("msc", "M.Sc.", "Master of Science", "M.Sc.", "Master of Science")

  lazy val beng = Degree(
    "beng",
    "B.Eng.",
    "Bachelor of Engineering",
    "B.Eng.",
    "Bachelor of Engineering"
  )

  lazy val meng = Degree(
    "meng",
    "M.Eng.",
    "Master of Engineering",
    "M.Eng.",
    "Master of Engineering"
  )

  implicit def fakeDegrees: Seq[Degree] =
    Seq(bsc, msc, beng, meng)
}
