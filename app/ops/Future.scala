package ops

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

private def abort[A](msg: String): Future[A] =
  Future.failed(new Exception(msg))

extension (self: Future[Boolean]) {
  infix def ||(other: => Future[Boolean])(using ExecutionContext): Future[Boolean] =
    self.flatMap(result => if result then Future.successful(true) else other)
}

extension [A](self: Future[A]) {
  def abortIf(pred: A => Boolean, msg: => String)(using ExecutionContext): Future[A] =
    self.flatMap(a => if (pred(a)) abort(msg) else Future.successful(a))

  def continueIf(pred: A => Boolean, msg: => String)(using ExecutionContext): Future[A] =
    self.abortIf(a => !pred(a), msg)

  def measure(tag: String)(using ExecutionContext): Future[A] =
    new FutureTimeTracker[A](self).track(tag)
}

extension [A](self: Future[Seq[A]]) {
  def single(using ExecutionContext): Future[A] =
    self.flatMap(xs =>
      xs.size match {
        case 1 =>
          Future.successful(xs.head)
        case 0 =>
          abort("expected one element, but found none")
        case _ =>
          abort(s"expected one element, but found: $xs")
      }
    )
}

extension [A](self: Future[Option[A]]) {
  def or(f: Future[Option[A]])(using ExecutionContext): Future[Option[A]] =
    self.flatMap(_.fold(f)(a => Future.successful(Some(a))))

  def orElse(f: Future[A])(using ExecutionContext): Future[A] =
    self.flatMap(_.fold(f)(a => Future.successful(a)))
}

extension [A](self: Future[Either[Throwable, A]]) {
  def unwrap(using ExecutionContext): Future[A] =
    self.flatMap {
      case Left(value)  => Future.failed(value)
      case Right(value) => Future.successful(value)
    }
}
