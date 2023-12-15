package ops

object StringBuilderOps {
  implicit class SBOps(private val self: StringBuilder) extends AnyVal {
    def appendOpt(s: Option[String]): StringBuilder =
      s.fold(self)(self.append)
  }
}
