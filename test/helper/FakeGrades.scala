package helper

import models.core.Grade

trait FakeGrades {
  lazy val bsc = Grade(
    "bsc",
    "B.Sc.",
    "Bachelor of Science",
    "B.Sc.",
    "Bachelor of Science"
  )

  lazy val msc =
    Grade("msc", "M.Sc.", "Master of Science", "M.Sc.", "Master of Science")

  lazy val beng = Grade(
    "beng",
    "B.Eng.",
    "Bachelor of Engineering",
    "B.Eng.",
    "Bachelor of Engineering"
  )

  lazy val meng = Grade(
    "meng",
    "M.Eng.",
    "Master of Engineering",
    "M.Eng.",
    "Master of Engineering"
  )

  implicit def fakeGrades: Seq[Grade] =
    Seq(bsc, msc, beng, meng)
}
