package database

import scala.concurrent.{ExecutionContext, Future}

package object repo {
  implicit class FutureOps[A](res: Future[Seq[A]]) {
    def single(implicit ctx: ExecutionContext): Future[A] =
      res.flatMap(xs =>
        if (xs.size > 1)
          Future.failed(new Throwable(s"expected one element, but found: $xs"))
        else
          Future.successful(xs.head)
      )
  }
}
