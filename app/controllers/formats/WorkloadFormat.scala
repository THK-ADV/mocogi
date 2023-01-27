package controllers.formats

import play.api.libs.json.{Format, Json}
import validator.Workload

trait WorkloadFormat {
  implicit val workloadFormat: Format[Workload] =
    Json.format[Workload]
}
