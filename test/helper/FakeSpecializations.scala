package helper

import models.core.Specialization

trait FakeSpecializations {

  lazy val sc = Specialization("inf1_sc", "label 1", "abbrev1", "inf1")

  lazy val vi = Specialization("wi1_vi", "label 2", "abbrev2", "wi1")

  lazy val az = Specialization("mi1_az", "label 3", "abbrev3", "mi1")

  lazy val ab = Specialization("itm1_ab", "label 4", "abbrev4", "itm1")

  implicit def fakeSpecializations: Seq[Specialization] = Seq(sc, vi, az, ab)
}
