package database.entities

import basedata.PersonStatus

case class PersonDbEntry(
    id: String,
    lastname: String,
    firstname: String,
    title: String,
    faculties: List[String],
    abbreviation: String,
    status: PersonStatus,
    kind: String,
)
