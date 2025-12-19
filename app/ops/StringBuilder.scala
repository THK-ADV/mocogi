package ops

extension (self: StringBuilder) {
  def appendOpt(s: Option[String]): StringBuilder =
    s.fold(self)(self.append)
}
