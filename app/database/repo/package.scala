package database

import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

package object repo {
  implicit class ActionSeqOps[A](private val self: DBIO[Seq[A]])
      extends AnyVal {
    def single(implicit ctx: ExecutionContext): DBIO[A] =
      self.flatMap(xs =>
        xs.size match {
          case 1 =>
            DBIO.successful(xs.head)
          case 0 =>
            DBIO.failed(new Throwable(s"expected one element, but found none"))
          case _ =>
            DBIO.failed(new Throwable(s"expected one element, but found: $xs"))
        }
      )
  }
}
