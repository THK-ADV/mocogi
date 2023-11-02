package ops

import scala.concurrent.{ExecutionContext, Future}

object FutureOps {

  def abort[A](msg: String): Future[A] =
    Future.failed(new Throwable(msg))

  implicit class Ops[A](val self: Future[A]) extends AnyVal {
    def abortIf(pred: A => Boolean, msg: => String)(implicit
        ctx: ExecutionContext
    ): Future[A] =
      self.flatMap(a =>
        if (pred(a)) Future.failed(new Throwable(msg)) else Future.successful(a)
      )
  }

  implicit class SeqOps[A](val self: Future[Seq[A]]) extends AnyVal {
    def single(implicit ctx: ExecutionContext): Future[A] =
      self.flatMap(xs =>
        if (xs.size > 1)
          Future.failed(new Throwable(s"expected one element, but found: $xs"))
        else
          Future.successful(xs.head)
      )
  }

  implicit class OptionOps[A](val self: Future[Option[A]]) extends AnyVal {
    def or(
        f: Future[Option[A]]
    )(implicit ctx: ExecutionContext): Future[Option[A]] =
      self.flatMap(_.fold(f)(a => Future.successful(Some(a))))

    def orElse(
        f: Future[A]
    )(implicit ctx: ExecutionContext): Future[A] =
      self.flatMap(_.fold(f)(a => Future.successful(a)))
  }
}
