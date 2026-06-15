package models.schedule

import java.util.UUID

import models.schedule.ScheduleEntrySeriesId.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.JsString
import play.api.libs.json.Json

final class ScheduleEntrySeriesIdSpec extends AnyWordSpec {

  "ScheduleEntrySeriesId JSON format" should {
    "read and write UUID strings without recursive UUID format lookup" in {
      val uuid = UUID.fromString("e935b457-472b-4bbb-b9ec-1a77e42793dd")

      val parsed = Json.parse(s""""$uuid"""").validate[ScheduleEntrySeriesId].get

      assert(parsed.toUUID == uuid)
      assert(Json.toJson(ScheduleEntrySeriesId(uuid)) == JsString(uuid.toString))
    }

    "reject invalid UUID strings" in {
      val parsed = Json.parse(""""not-a-uuid"""").validate[ScheduleEntrySeriesId]

      assert(parsed.isError)
    }

    "read schedule entry payloads with a series id" in {
      val seriesId = UUID.fromString("7f9ece92-15e3-44b7-99bd-c1ddb7a92942")
      val payload  = Json.obj(
        "id"         -> "f9b65d04-2b18-4728-b0f6-45ec48218262",
        "seriesId"   -> seriesId.toString,
        "module"     -> "4bb3ef97-af80-42b5-a795-3cb1429d2c4e",
        "courseType" -> "lecture",
        "rooms"      -> Json.arr("87bfcc4f-62bb-459f-a0d9-f5c2ba67f03c"),
        "start"      -> "2026-04-22T07:00:00.000Z",
        "end"        -> "2026-04-22T09:00:00.000Z",
        "props"      -> Json.obj()
      )

      val entry = payload.validate[ScheduleEntry.JSON].get

      assert(entry.seriesId.toUUID == seriesId)
    }
  }
}
