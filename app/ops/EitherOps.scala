package ops

import scala.concurrent.Future

object EitherOps {
  implicit class EOps[A, B](private val self: Either[A, B]) extends AnyVal {
    def bimap[A1, B1](left: A => A1, right: B => B1): Either[A1, B1] =
      self match {
        case Left(a)  => Left(left(a))
        case Right(b) => Right(right(b))
      }

    def mapErr[A1](left: A => A1): Either[A1, B] =
      self match {
        case Left(value)  => Left(left(value))
        case Right(value) => Right(value)
      }
  }

  implicit class EThrowableOps[A](private val self: Either[Throwable, A])
      extends AnyVal {
    def toFuture: Future[A] =
      self match {
        case Left(value)  => Future.failed(value)
        case Right(value) => Future.successful(value)
      }
  }

  implicit class EStringThrowOps[A](private val self: Either[String, A])
      extends AnyVal {
    def toFuture: Future[A] =
      self match {
        case Left(value)  => Future.failed(new Throwable(value))
        case Right(value) => Future.successful(value)
      }
  }
}
