package database

import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

package object repo {
  implicit class ActionSeqOps[A](val self: DBIO[Seq[A]]) extends AnyVal {
    def single(implicit ctx: ExecutionContext): DBIO[A] =
      self.flatMap(xs =>
        if (xs.size > 1)
          DBIO.failed(new Throwable(s"expected one element, but found: $xs"))
        else
          DBIO.successful(xs.head)
      )
  }
}
