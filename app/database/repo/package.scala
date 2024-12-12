package database

import scala.concurrent.ExecutionContext

import slick.dbio.DBIO

package object repo {
  extension [A](self: DBIO[Seq[A]]) {
    def single(implicit ctx: ExecutionContext): DBIO[A] =
      self.flatMap(xs =>
        xs.size match {
          case 1 =>
            DBIO.successful(xs.head)
          case 0 =>
            DBIO.failed(new Throwable("expected one element, but found none"))
          case _ =>
            DBIO.failed(new Throwable(s"expected one element, but found: $xs"))
        }
      )

    def singleOpt(implicit ctx: ExecutionContext): DBIO[Option[A]] =
      self.flatMap(xs =>
        xs.size match {
          case 1 => DBIO.successful(Some(xs.head))
          case _ => DBIO.successful(None)
        }
      )
  }
}
