package parsing.types

import models.core.Person

case class Responsibilities(
    moduleManagement: List[Person],
    lecturers: List[Person]
)
