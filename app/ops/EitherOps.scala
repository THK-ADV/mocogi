package ops

object EitherOps {
  implicit class EOps[A, B](e: Either[A, B]) {
    def bimap[A1, B1](left: A => A1, right: B => B1): Either[A1, B1] =
      e match {
        case Left(a)  => Left(left(a))
        case Right(b) => Right(right(b))
      }
  }
}
