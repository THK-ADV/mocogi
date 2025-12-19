package database.table

import models.PeopleImage
import slick.jdbc.PostgresProfile.api.*

private[database] final class PeopleImagesTable(tag: Tag) extends Table[PeopleImage](tag, "people_images") {

  def person = column[String]("person", O.PrimaryKey)

  def imageUrl = column[String]("image_url")

  override def * = (person, imageUrl) <> (PeopleImage.apply, PeopleImage.unapply)
}
