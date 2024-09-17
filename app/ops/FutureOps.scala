package ops

import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

object FutureOps {

  def abort[A](msg: String): Future[A] =
    Future.failed(new Exception(msg))

  implicit class Ops[A](private val self: Future[A]) extends AnyVal {
    def abortIf(pred: A => Boolean, msg: => String)(implicit
        ctx: ExecutionContext
    ): Future[A] =
      self.flatMap(a => if (pred(a)) abort(msg) else Future.successful(a))

    def continueIf(pred: A => Boolean, msg: => String)(implicit
        ctx: ExecutionContext
    ): Future[A] = self.abortIf(a => !pred(a), msg)

    @unused
    def measure(tag: String)(implicit ctx: ExecutionContext): Future[A] =
      new FutureTimeTracker[A](self).track(tag)
  }

  implicit class SeqOps[A](private val self: Future[Seq[A]]) extends AnyVal {
    def single(implicit ctx: ExecutionContext): Future[A] = {
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
  }

  implicit class OptionOps[A](private val self: Future[Option[A]])
      extends AnyVal {
    def or(
        f: Future[Option[A]]
    )(implicit ctx: ExecutionContext): Future[Option[A]] =
      self.flatMap(_.fold(f)(a => Future.successful(Some(a))))

    @unused
    def orElse(
        f: Future[A]
    )(implicit ctx: ExecutionContext): Future[A] =
      self.flatMap(_.fold(f)(a => Future.successful(a)))
  }

  implicit class EitherOps[A](private val self: Future[Either[Throwable, A]])
      extends AnyVal {
    def unwrap(implicit ctx: ExecutionContext): Future[A] =
      self.flatMap {
        case Left(value)  => Future.failed(value)
        case Right(value) => Future.successful(value)
      }
  }
}
