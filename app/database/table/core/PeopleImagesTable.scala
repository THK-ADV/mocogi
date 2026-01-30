package database.table.core

import database.Schema
import models.PeopleImage
import slick.jdbc.PostgresProfile.api.*

private[database] final class PeopleImagesTable(tag: Tag)
    extends Table[PeopleImage](tag, Some(Schema.Core.name), "people_images") {

  def person = column[String]("person", O.PrimaryKey)

  def imageUrl = column[String]("image_url")

  override def * = (person, imageUrl) <> (PeopleImage.apply, PeopleImage.unapply)
}
