package ops

object EitherOps {
  implicit class EOps[A, B](e: Either[A, B]) {
    def biFlatMap[A0, A1, B1](
        left: A => A0,
        newLeft: A1 => A0,
        right: B => Either[A1, B1]
    ): Either[A0, B1] = e match {
      case Left(a) => Left(left(a))
      case Right(b) =>
        right(b) match {
          case Left(a1)  => Left(newLeft(a1))
          case Right(b1) => Right(b1)
        }
    }

    def mapLeft[A1](f: A => A1): Either[A1, B] = e match {
      case Right(b) => Right(b)
      case Left(a)  => Left(f(a))
    }
  }
}
