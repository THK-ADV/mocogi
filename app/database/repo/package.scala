package database

import scala.concurrent.ExecutionContext

import slick.dbio.DBIO

package object repo {
  extension [A](self: DBIO[Seq[A]]) {
    def single(using ExecutionContext): DBIO[A] =
      self.flatMap(xs =>
        xs.size match {
          case 1 =>
            DBIO.successful(xs.head)
          case 0 =>
            DBIO.failed(new Exception("expected one element, but found none"))
          case _ =>
            DBIO.failed(new Exception(s"expected one element, but found: $xs"))
        }
      )

    def singleOpt(using ExecutionContext): DBIO[Option[A]] =
      self.flatMap(xs =>
        xs.size match {
          case 1 => DBIO.successful(Some(xs.head))
          case _ => DBIO.successful(None)
        }
      )
  }
}
