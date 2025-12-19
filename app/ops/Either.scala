package ops

import scala.annotation.targetName
import scala.concurrent.Future

extension [A, B](self: Either[A, B]) {
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

extension [A](self: Either[Throwable, A]) {
  @targetName("toFutureThrowable")
  def toFuture: Future[A] =
    self match {
      case Left(value)  => Future.failed(value)
      case Right(value) => Future.successful(value)
    }
}

extension [A](self: Either[String, A]) {
  @targetName("toFutureString")
  def toFuture: Future[A] =
    self match {
      case Left(value)  => Future.failed(new Exception(value))
      case Right(value) => Future.successful(value)
    }
}
