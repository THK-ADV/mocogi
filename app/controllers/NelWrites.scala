package controllers

import cats.data.NonEmptyList
import play.api.libs.json.{JsError, JsSuccess, Reads, Writes}

trait NelWrites {
  implicit def nelWrites[A](implicit w: Writes[A]): Writes[NonEmptyList[A]] =
    Writes.list[A].contramap(_.toList)

  implicit def nelReads[A](implicit r: Reads[A]): Reads[NonEmptyList[A]] =
    Reads
      .list[A]
      .flatMapResult(xs =>
        if (xs.isEmpty) JsError("expected non-empty list")
        else JsSuccess(NonEmptyList.fromListUnsafe(xs))
      )
}
