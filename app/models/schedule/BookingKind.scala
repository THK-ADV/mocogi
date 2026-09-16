package models.schedule

import play.api.libs.json.Format
import play.api.libs.json.Reads
import play.api.libs.json.Writes

enum BookingKind(val id: String) {
  case Teaching extends BookingKind("teaching")
  case Campus   extends BookingKind("campus")
  case Faculty  extends BookingKind("faculty")
}

object BookingKind {
  given Format[BookingKind] = Format.of[String].bimap(apply, _.id)

  def apply(id: String): BookingKind =
    id match {
      case "teaching" => Teaching
      case "campus"   => Campus
      case "faculty"  => Faculty
    }
}
