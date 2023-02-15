package controllers.formats

import parsing.types.Participants
import play.api.libs.json.{Format, Json}

trait ParticipantsFormat {
  implicit val participantsFormat: Format[Participants] =
    Json.format[Participants]
}
