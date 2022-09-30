package parsing.types

import basedata.Person

case class Responsibilities(
    moduleManagement: List[Person],
    lecturers: List[Person]
)
