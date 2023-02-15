package controllers

import play.api.libs.json.{Format, JsResult, JsValue, Reads}

import scala.util.Try

package object formats {
  implicit class FormatOps[A](fmt: Format[A]) {
    def bimapTry[B](read: A => Try[B], write: B => A): Format[B] =
      new Format[B] {
        override def reads(json: JsValue): JsResult[B] =
          fmt.reads(json).flatMap(a => JsResult.fromTry(read(a)))

        override def writes(b: B): JsValue =
          fmt.writes(write(b))
      }
  }

  implicit def listReads[A](implicit reads: Reads[A]): Reads[List[A]] = Reads.list(reads)
}
