package helper

trait FakeStudyPrograms {
  lazy val inf = "inf_inf"

  lazy val itm = "inf_itm"

  lazy val mi = "inf_mi"

  lazy val wi = "inf_wi"

  lazy val mim = "inf_mim"

  implicit def fakeStudyProgramPreviews: Seq[String] =
    Seq(inf, itm, mi, wi, mim)
}
