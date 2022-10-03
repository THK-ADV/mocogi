package helper

import basedata.Grade

trait FakeGrades {
  implicit def fakeGrades: Seq[Grade] = Seq(
    Grade("bsc"),
    Grade("msc"),
    Grade("beng"),
    Grade("meng"),
  )
}
